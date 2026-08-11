package cn.bugstack.ai.rag.retriever;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RrfFusionTest {

    private ScoredChunk chunk(long id, String title, double score) {
        return ScoredChunk.builder().chunkId(id).docTitle(title).score(score).build();
    }

    @Test
    public void testFuseCombinesRanks() {
        List<ScoredChunk> vector = Arrays.asList(chunk(1, "A", 0.9), chunk(2, "B", 0.8), chunk(3, "C", 0.7));
        List<ScoredChunk> fulltext = Arrays.asList(chunk(3, "C", 0.5), chunk(1, "A", 0.4));
        List<ScoredChunk> fused = RrfFusion.fuse(vector, fulltext, 60, 5);
        assertEquals(3, fused.size());
        // 同时出现在两路的 chunk 排名应更高
        assertEquals(Long.valueOf(1), fused.get(0).getChunkId());
        assertTrue(fused.get(0).getScore() > fused.get(2).getScore());
    }

    @Test
    public void testTopK() {
        List<ScoredChunk> vector = Arrays.asList(chunk(1, "A", 0.9), chunk(2, "B", 0.8), chunk(3, "C", 0.7));
        List<ScoredChunk> fused = RrfFusion.fuse(vector, Arrays.asList(), 60, 2);
        assertEquals(2, fused.size());
    }

}