package cn.bugstack.ai.chat;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.llm.LlmClient;
import cn.bugstack.ai.rag.retriever.HybridRetriever;
import cn.bugstack.ai.rag.retriever.Reranker;
import cn.bugstack.ai.rag.retriever.RetrievalCache;
import cn.bugstack.ai.rag.retriever.ScoredChunk;
import cn.bugstack.ai.rag.repository.ChatLogRepository;
import cn.bugstack.ai.tool.ToolRegistry;
import cn.bugstack.ai.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 单轮客服问答编排（Phase1-4 整合）：
 * 意图分析 → (工具/动态数据) → 混合检索+缓存 → 重排 → FAQ直出或 LLM 生成 → 引用溯源 → 全链路日志
 */
@Slf4j
@Service
public class ChatService {

    private final QueryAnalyzer queryAnalyzer;
    private final HybridRetriever hybridRetriever;
    private final RetrievalCache retrievalCache;
    private final Reranker reranker;
    private final LlmClient llmClient;
    private final ChatLogRepository chatLogRepository;
    private final ToolRegistry toolRegistry;
    private final FaqDirectService faqDirectService;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public ChatService(QueryAnalyzer queryAnalyzer,
                       HybridRetriever hybridRetriever,
                       RetrievalCache retrievalCache,
                       Reranker reranker,
                       LlmClient llmClient,
                       ChatLogRepository chatLogRepository,
                       ToolRegistry toolRegistry,
                       FaqDirectService faqDirectService,
                       ObjectMapper objectMapper,
                       AiProperties properties) {
        this.queryAnalyzer = queryAnalyzer;
        this.hybridRetriever = hybridRetriever;
        this.retrievalCache = retrievalCache;
        this.reranker = reranker;
        this.llmClient = llmClient;
        this.chatLogRepository = chatLogRepository;
        this.toolRegistry = toolRegistry;
        this.faqDirectService = faqDirectService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public ChatResponse chat(String userId, String question, String sessionId) {
        long start = System.currentTimeMillis();
        String messageId = UUID.randomUUID().toString().replace("-", "");
        QueryPlan plan = queryAnalyzer.analyze(userId, question);

        String answer = null;
        boolean needHuman = false;
        List<Reference> references = new ArrayList<>();
        String dynamicContext = null;

        if (plan.getIntent() == Intent.OTHER) {
            answer = AiPrompts.HUMAN_MESSAGE;
            needHuman = true;
        } else {
            // 状态类：启用工具则尝试实时数据，否则人工处理中
            if (plan.isNeedTool()) {
                if (Boolean.TRUE.equals(properties.getTool().getEnabled())) {
                    ToolResult toolResult = toolRegistry.tryExecute(question, plan);
                    if (toolResult != null && toolResult.isSuccess()) {
                        dynamicContext = toolResult.getData();
                    } else {
                        answer = AiPrompts.HUMAN_MESSAGE;
                        needHuman = true;
                    }
                } else {
                    answer = AiPrompts.HUMAN_MESSAGE;
                    needHuman = true;
                }
            }

            if (answer == null) {
                List<ScoredChunk> chunks = retrievalCache.get(plan.getCategoryFilter(), plan.getRewrittenQuery());
                if (chunks == null) {
                    chunks = hybridRetriever.retrieve(plan.getRewrittenQuery(), plan.getCategoryFilter());
                    retrievalCache.put(plan.getCategoryFilter(), plan.getRewrittenQuery(), chunks);
                }
                chunks = reranker.rerank(plan.getRewrittenQuery(), chunks, properties.getRetrieval().getResultTopK());
                // 父子 chunk：命中子块 → 展开为父块全文（上下文完整）
                chunks = hybridRetriever.expandParents(chunks);

                if (chunks.isEmpty()) {
                    answer = AiPrompts.HUMAN_MESSAGE;
                    needHuman = true;
                } else {
                    Optional<String> faqDirect = Boolean.TRUE.equals(properties.getOptimization().getFaqDirect())
                            ? faqDirectService.extract(chunks.get(0))
                            : Optional.empty();
                    if (faqDirect.isPresent()) {
                        answer = faqDirect.get();
                    } else {
                        String raw = llmClient.chat(AiPrompts.SYSTEM, buildUserPrompt(plan, chunks, dynamicContext));
                        Optional<StructuredAnswer> structured = StructuredOutputParser.parse(raw, objectMapper);
                        if (structured.isPresent()) {
                            answer = structured.get().getAnswer();
                            needHuman = structured.get().isNeedHuman();
                        } else {
                            answer = raw;
                        }
                        if (needHuman) {
                            answer = AiPrompts.HUMAN_MESSAGE;
                        }
                    }
                    for (ScoredChunk chunk : chunks) {
                        references.add(Reference.builder()
                                .title(chunk.getDocTitle())
                                .sectionPath(chunk.getSectionPath())
                                .build());
                    }
                }
            }
        }

        long latency = System.currentTimeMillis() - start;
        logChat(messageId, userId, sessionId, question, plan, answer, needHuman, references, latency);

        return ChatResponse.builder()
                .messageId(messageId)
                .answer(answer)
                .references(references)
                .needHuman(needHuman)
                .build();
    }

    private String buildUserPrompt(QueryPlan plan, List<ScoredChunk> chunks, String dynamicContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(plan.getRewrittenQuery()).append("\n\n");
        if (dynamicContext != null && !dynamicContext.isEmpty()) {
            sb.append("【实时数据】\n").append(dynamicContext).append("\n\n");
        }
        sb.append("【参考资料】\n");
        for (int i = 0; i < chunks.size(); i++) {
            ScoredChunk chunk = chunks.get(i);
            sb.append("[").append(i + 1).append("] ");
            if (chunk.getDocTitle() != null) {
                sb.append(chunk.getDocTitle());
            }
            if (chunk.getSectionPath() != null) {
                sb.append(" / ").append(chunk.getSectionPath());
            }
            sb.append("\n").append(chunk.getChunkText()).append("\n\n");
        }
        return sb.toString();
    }

    private void logChat(String messageId, String userId, String sessionId, String question,
                         QueryPlan plan, String answer, boolean needHuman,
                         List<Reference> references, long latency) {
        try {
            List<Map<String, String>> refs = new ArrayList<>();
            for (Reference ref : references) {
                Map<String, String> item = new HashMap<>();
                item.put("title", ref.getTitle());
                item.put("sectionPath", ref.getSectionPath());
                refs.add(item);
            }
            chatLogRepository.insert(messageId, userId, sessionId, question,
                    plan.getIntent().name(), needHuman, answer, refs, latency);
        } catch (Exception e) {
            log.warn("会话日志写入失败 messageId={}", messageId, e);
        }
    }

}