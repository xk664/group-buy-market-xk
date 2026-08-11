package cn.bugstack.ai.rag.retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion）融合：
 * score(d) = Σ 1/(k + rank_i(d))，解决向量/全文分数不可比的问题
 */
public final class RrfFusion {

    private RrfFusion() {
    }

    public static List<ScoredChunk> fuse(List<ScoredChunk> vectorResults,
                                         List<ScoredChunk> fulltextResults,
                                         int k,
                                         int topK) {
        Map<Long, ScoredChunk> aggregated = new LinkedHashMap<>();
        addRanks(aggregated, vectorResults, k);
        addRanks(aggregated, fulltextResults, k);

        List<ScoredChunk> sorted = new ArrayList<>(aggregated.values());
        sorted.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        return sorted.size() > topK ? sorted.subList(0, topK) : sorted;
    }

    private static void addRanks(Map<Long, ScoredChunk> aggregated, List<ScoredChunk> results, int k) {
        for (int i = 0; i < results.size(); i++) {
            ScoredChunk item = results.get(i);
            ScoredChunk agg = aggregated.computeIfAbsent(item.getChunkId(), id -> copyOf(item));
            agg.setScore(agg.getScore() + 1.0 / (k + i + 1));
        }
    }

    private static ScoredChunk copyOf(ScoredChunk source) {
        return ScoredChunk.builder()
                .chunkId(source.getChunkId())
                .docId(source.getDocId())
                .docTitle(source.getDocTitle())
                .chunkText(source.getChunkText())
                .sectionPath(source.getSectionPath())
                .metadataJson(source.getMetadataJson())
                .score(0.0)
                .build();
    }

}