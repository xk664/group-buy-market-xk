package cn.bugstack.ai.rag.retriever;

import cn.bugstack.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DashScopeRerankerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScoredChunk chunk(long id, String text, double score) {
        return ScoredChunk.builder().chunkId(id).chunkText(text).score(score).build();
    }

    @Test
    public void testApplyScoresReorderAndTopK() throws Exception {
        List<ScoredChunk> candidates = Arrays.asList(
                chunk(1, "拼团失败自动退款", 0.1),
                chunk(2, "退款1-3个工作日", 0.2),
                chunk(3, "耳机续航36小时", 0.3),
                chunk(4, "保温杯316材质", 0.4));
        String json = "{\"output\":{\"results\":[" +
                "{\"index\":3,\"relevance_score\":0.9}," +
                "{\"index\":1,\"relevance_score\":0.8}," +
                "{\"index\":0,\"relevance_score\":0.7}" +
                "]}}";
        List<ScoredChunk> result = DashScopeReranker.applyScores(candidates, json, objectMapper, 2);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(4), result.get(0).getChunkId());
        assertEquals(Long.valueOf(2), result.get(1).getChunkId());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
    }

    @Test
    public void testPlaceholderKeyUnavailable() {
        AiProperties properties = new AiProperties();
        properties.getLlm().setApiKey("sk-<你的DashScope密钥>");
        properties.getAnalyzer().setRerankMode("cross-encoder");
        DashScopeReranker reranker = new DashScopeReranker(properties, null, objectMapper);
        assertEquals(false, reranker.available());
    }

    @Test
    public void testRerankerServiceFallbackToScore() {
        AiProperties properties = new AiProperties();
        properties.getLlm().setApiKey("sk-<你的DashScope密钥>");
        properties.getAnalyzer().setRerankMode("cross-encoder");
        RerankerService service = new RerankerService(properties, null, objectMapper);
        List<ScoredChunk> candidates = new ArrayList<ScoredChunk>(Arrays.asList(
                chunk(1, "a", 0.3), chunk(2, "b", 0.9), chunk(3, "c", 0.6)));
        List<ScoredChunk> result = service.rerank("测试", candidates, 3);
        assertEquals(Long.valueOf(2), result.get(0).getChunkId());
        assertEquals(Long.valueOf(3), result.get(1).getChunkId());
    }

}