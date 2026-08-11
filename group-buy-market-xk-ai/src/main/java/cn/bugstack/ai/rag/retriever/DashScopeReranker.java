package cn.bugstack.ai.rag.retriever;

import cn.bugstack.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云端 Cross-Encoder 重排（DashScope text-rerank，默认模型 bge-reranker-v2-m3）：
 * query + 候选文档 逐对打分，返回按 relevance_score 重排后的 Top-K
 * API Key 未配置/占位符时 available()=false，由调用方回退分数重排
 */
@Slf4j
public class DashScopeReranker {

    private static final String PLACEHOLDER = "sk-<你的DashScope密钥>";

    private final AiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DashScopeReranker(AiProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean available() {
        String key = properties.getLlm().getApiKey();
        return key != null && !key.trim().isEmpty() && !PLACEHOLDER.equals(key.trim());
    }

    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getAnalyzer().getRerankModel());
        ObjectNode input = body.putObject("input");
        input.put("query", query);
        ArrayNode documents = input.putArray("documents");
        for (ScoredChunk candidate : candidates) {
            documents.add(candidate.getChunkText() == null ? "" : candidate.getChunkText());
        }
        input.put("top_n", topK);
        body.putObject("parameters").put("return_documents", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getLlm().getApiKey().trim());

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    properties.getAnalyzer().getRerankBaseUrl(),
                    new HttpEntity<String>(body.toString(), headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("rerank HTTP " + resp.getStatusCode() + ": " + resp.getBody());
            }
            return applyScores(candidates, resp.getBody(), objectMapper, topK);
        } catch (Exception e) {
            throw new IllegalStateException("调用云端重排失败", e);
        }
    }

    /** 解析 rerank 响应并按 relevance_score 重排（纯函数，便于单测） */
    static List<ScoredChunk> applyScores(List<ScoredChunk> candidates, String responseBody,
                                         ObjectMapper objectMapper, int topK) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("output").path("results");
        Map<Integer, Double> scores = new HashMap<Integer, Double>();
        for (JsonNode item : results) {
            scores.put(item.path("index").asInt(), item.path("relevance_score").asDouble());
        }
        List<ScoredChunk> scored = new ArrayList<ScoredChunk>();
        for (int i = 0; i < candidates.size(); i++) {
            ScoredChunk candidate = candidates.get(i);
            Double score = scores.get(i);
            if (score != null) {
                candidate.setScore(score);
                scored.add(candidate);
            }
        }
        scored.sort(java.util.Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

}