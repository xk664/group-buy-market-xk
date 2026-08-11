package cn.bugstack.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 解析 LLM 的 JSON 回答 {answer, need_human}；非 JSON 时返回空，调用方使用原文
 */
public final class StructuredOutputParser {

    private StructuredOutputParser() {
    }

    public static Optional<StructuredAnswer> parse(String raw, ObjectMapper objectMapper) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return Optional.empty();
            }
            JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));
            String answer = node.path("answer").asText(null);
            if (answer == null || answer.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(StructuredAnswer.builder()
                    .answer(answer.trim())
                    .needHuman(node.path("need_human").asBoolean(false))
                    .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}