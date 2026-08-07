package cn.bugstack.ai.rag.retriever;

import cn.bugstack.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 重排服务：
 * - score（默认）：按 RRF/相似度分数排序
 * - cross-encoder：调用云端 bge-reranker-v2-m3 逐对打分重排；
 *   API Key 未配置或调用失败时自动回退分数重排，保证链路不挂
 */
@Slf4j
@Service
public class RerankerService implements Reranker {

    private final AiProperties properties;
    private final DashScopeReranker dashScopeReranker;

    public RerankerService(AiProperties properties,
                           RestTemplate aiRestTemplate,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.dashScopeReranker = new DashScopeReranker(properties, aiRestTemplate, objectMapper);
    }

    @Override
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<ScoredChunk>();
        }
        boolean crossEncoder = "cross-encoder".equals(properties.getAnalyzer().getRerankMode());
        if (crossEncoder && dashScopeReranker.available()) {
            try {
                long start = System.currentTimeMillis();
                List<ScoredChunk> result = dashScopeReranker.rerank(query, candidates, topK);
                log.info("cross-encoder 重排完成 candidates={} topK={} 耗时={}ms",
                        candidates.size(), result.size(), System.currentTimeMillis() - start);
                return result;
            } catch (Exception e) {
                log.warn("cross-encoder 重排失败，回退分数重排: {}", e.getMessage());
            }
        } else if (crossEncoder) {
            log.info("cross-encoder 模式但 API Key 未配置，使用分数重排（配置 AI_LLM_API_KEY 后生效）");
        }
        return scoreSort(candidates, topK);
    }

    private List<ScoredChunk> scoreSort(List<ScoredChunk> candidates, int topK) {
        List<ScoredChunk> sorted = new ArrayList<ScoredChunk>(candidates);
        sorted.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        return sorted.size() > topK ? sorted.subList(0, topK) : sorted;
    }

}