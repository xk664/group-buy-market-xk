package cn.bugstack.ai.chat;

import cn.bugstack.ai.rag.retriever.ScoredChunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * FAQ 直出（成本优化）：检索 Top-1 命中 FAQ 文档时，直接抽取 A: 回答，跳过 LLM 调用
 */
@Service
public class FaqDirectService {

    private final ObjectMapper objectMapper;

    public FaqDirectService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<String> extract(ScoredChunk chunk) {
        if (chunk == null || chunk.getMetadataJson() == null) {
            return Optional.empty();
        }
        try {
            JsonNode meta = objectMapper.readTree(chunk.getMetadataJson());
            if (!"FAQ".equals(meta.path("doc_type").asText())) {
                return Optional.empty();
            }
            String text = chunk.getChunkText();
            int idx = text.indexOf("A:");
            if (idx < 0) {
                idx = text.indexOf("A：");
            }
            if (idx < 0) {
                return Optional.empty();
            }
            String answer = text.substring(idx + 2).trim();
            return answer.isEmpty() ? Optional.empty() : Optional.of(answer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}