package cn.bugstack.ai.rag.retriever;

import java.util.List;

/**
 * 检索结果重排：召回 Top-10 → 重排 Top-5
 */
public interface Reranker {

    List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topK);

}