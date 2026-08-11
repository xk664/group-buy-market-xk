package cn.bugstack.ai.chat;

import cn.bugstack.ai.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * LLM 查询分析（Phase2）：一次调用输出意图/实体/改写查询，失败自动回退规则分析
 */
@Slf4j
public class LlmQueryAnalyzer {

    private static final String SYSTEM = "你只输出 JSON，不要输出任何其他内容。";
    private static final String PROMPT =
            "你是客服查询分析器。分析用户问题，只输出 JSON：\n" +
            "{\"intent\":\"REFUND|PRODUCT|GROUPON|OTHER\",\"need_tool\":true或false," +
            "\"category_filter\":\"REFUND|PRODUCT|GROUPON|null\",\"rewritten_query\":\"改写后的标准问法\",\"entities\":{}}\n" +
            "规则：涉及个人订单/退款进度/拼团进度等实时状态 → need_tool=true；投诉/闲聊 → intent=OTHER；" +
            "否则按退款/商品/拼团意图分类并给出 category_filter。\n用户问题：";

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final RuleQueryAnalyzer fallback;

    public LlmQueryAnalyzer(LlmClient llmClient, ObjectMapper objectMapper, RuleQueryAnalyzer fallback) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    public QueryPlan analyze(String userId, String question) {
        try {
            String raw = llmClient.chat(SYSTEM, PROMPT + question);
            JsonNode node = objectMapper.readTree(extractJson(raw));
            Intent intent;
            try {
                intent = Intent.valueOf(node.path("intent").asText("OTHER"));
            } catch (Exception e) {
                intent = Intent.OTHER;
            }
            boolean needTool = node.path("need_tool").asBoolean(false);
            String category = node.path("category_filter").asText(null);
            if (category == null || "null".equalsIgnoreCase(category.trim())) {
                category = null;
            }
            String rewritten = node.path("rewritten_query").asText(question);
            return QueryPlan.builder()
                    .intent(intent)
                    .categoryFilter(category)
                    .rewrittenQuery(rewritten)
                    .needTool(needTool)
                    .entities(new HashMap<>())
                    .build();
        } catch (Exception e) {
            log.warn("LLM 查询分析失败，回退规则分析: {}", e.getMessage());
            return fallback.analyze(userId, question);
        }
    }

    public boolean isMock() {
        return llmClient.isMock();
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

}