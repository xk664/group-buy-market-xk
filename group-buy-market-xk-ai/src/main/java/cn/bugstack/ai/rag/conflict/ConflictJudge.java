package cn.bugstack.ai.rag.conflict;

import cn.bugstack.ai.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 冲突判断器：把两段知识文本交给 LLM，判断是否同主题且互相矛盾
 * Mock LLM 模式下不判断（返回空），避免误报
 */
@Slf4j
public class ConflictJudge {

    private static final String SYSTEM = "你只输出 JSON，不要输出任何其他内容。";
    private static final String PROMPT =
            "你是客服知识冲突检测器。判断两段客服知识文本：\n" +
            "1. related：是否在讲同一件事（同一主题）；\n" +
            "2. conflict：说法是否互相矛盾（数字/时效/金额/是否类硬冲突）；\n" +
            "3. topic：主题一句话；\n" +
            "4. reason：矛盾原因。\n" +
            "只输出 JSON：{\"related\":true或false,\"conflict\":true或false,\"topic\":\"...\",\"reason\":\"...\"}\n\n" +
            "文本A：\n%s\n\n文本B：\n%s";

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ConflictJudge(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public Optional<Verdict> judge(String textA, String textB) {
        if (llmClient.isMock()) {
            return Optional.empty();
        }
        try {
            String raw = llmClient.chat(SYSTEM, String.format(PROMPT, textA, textB));
            JsonNode node = objectMapper.readTree(extractJson(raw));
            boolean related = node.path("related").asBoolean(false);
            boolean conflict = node.path("conflict").asBoolean(false);
            if (!related || !conflict) {
                return Optional.empty();
            }
            return Optional.of(new Verdict(true,
                    node.path("topic").asText(""),
                    node.path("reason").asText("")));
        } catch (Exception e) {
            log.warn("冲突判断失败，跳过该对: {}", e.getMessage());
            return Optional.empty();
        }
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

    @Data
    @AllArgsConstructor
    public static class Verdict {
        private boolean conflict;
        private String topic;
        private String reason;
    }

}