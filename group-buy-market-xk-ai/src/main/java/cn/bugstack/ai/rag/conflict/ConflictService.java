package cn.bugstack.ai.rag.conflict;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.llm.LlmClient;
import cn.bugstack.ai.rag.DocStatus;
import cn.bugstack.ai.rag.embedding.EmbeddingService;
import cn.bugstack.ai.rag.repository.ConflictRepository;
import cn.bugstack.ai.rag.repository.KnowledgeRepository;
import cn.bugstack.ai.rag.retriever.ScoredChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 冲突检测服务：
 * 1. 对目标文档每个 chunk 用 embedding 找相似 chunk（排除同文档）
 * 2. LLM 判断是否同主题且矛盾
 * 3. 冲突记录入库，文档标记 CONFLICT（被检索拦截）
 */
@Slf4j
@Service
public class ConflictService {

    private final KnowledgeRepository knowledgeRepository;
    private final ConflictRepository conflictRepository;
    private final EmbeddingService embeddingService;
    private final ConflictJudge conflictJudge;
    private final AiProperties properties;

    public ConflictService(KnowledgeRepository knowledgeRepository,
                           ConflictRepository conflictRepository,
                           EmbeddingService embeddingService,
                           LlmClient llmClient,
                           ObjectMapper objectMapper,
                           AiProperties properties) {
        this.knowledgeRepository = knowledgeRepository;
        this.conflictRepository = conflictRepository;
        this.embeddingService = embeddingService;
        this.conflictJudge = new ConflictJudge(llmClient, objectMapper);
        this.properties = properties;
    }

    /** 检测单个文档，返回发现的冲突数；有冲突则文档置为 CONFLICT */
    public int detectForDoc(long docId) {
        if (!Boolean.TRUE.equals(properties.getConflict().getEnabled())) {
            return 0;
        }
        List<ScoredChunk> chunks = knowledgeRepository.listChunksByDoc(docId);
        int found = 0;
        for (ScoredChunk chunk : chunks) {
            String vec = EmbeddingService.toVectorLiteral(embeddingService.embed(chunk.getChunkText()));
            List<ScoredChunk> similar = knowledgeRepository.searchSimilarChunks(
                    vec, docId, properties.getConflict().getThreshold(), properties.getConflict().getTopK());
            for (ScoredChunk sim : similar) {
                if (sim.getChunkId().equals(chunk.getChunkId())) {
                    continue;
                }
                Optional<ConflictJudge.Verdict> verdict = conflictJudge.judge(chunk.getChunkText(), sim.getChunkText());
                if (!verdict.isPresent() || !verdict.get().isConflict()) {
                    continue;
                }
                // 规范化：chunk 对按 id 升序存储，配合 UNIQUE 去重
                long chunkA = chunk.getChunkId();
                long chunkB = sim.getChunkId();
                long docA = chunk.getDocId();
                long docB = sim.getDocId();
                if (chunkB < chunkA) {
                    long tmp = chunkA;
                    chunkA = chunkB;
                    chunkB = tmp;
                    tmp = docA;
                    docA = docB;
                    docB = tmp;
                }
                conflictRepository.insert(docA, chunkA, docB, chunkB,
                        verdict.get().getTopic(), verdict.get().getReason(), "HIGH");
                found++;
            }
        }
        if (found > 0) {
            knowledgeRepository.setDocStatus(docId, DocStatus.CONFLICT.name());
            log.warn("文档 {} 检测到 {} 处冲突，已标记 CONFLICT", docId, found);
        }
        return found;
    }

    /** 全量检测所有 ACTIVE 文档 */
    public int detectAll() {
        List<Long> ids = knowledgeRepository.listActiveDocIds();
        int total = 0;
        for (Long id : ids) {
            total += detectForDoc(id);
        }
        return total;
    }

    public List<ConflictRepository.ConflictRecord> list(String status, int limit) {
        return conflictRepository.listByStatus(status, limit);
    }

    /** 人工确认：RESOLVED（已修复）/ IGNORED（误报） */
    public boolean resolve(long id, String action) {
        return conflictRepository.resolve(id, action) > 0;
    }

}