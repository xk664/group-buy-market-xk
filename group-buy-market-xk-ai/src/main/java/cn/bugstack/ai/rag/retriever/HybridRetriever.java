package cn.bugstack.ai.rag.retriever;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.rag.TextSearchUtil;
import cn.bugstack.ai.rag.embedding.EmbeddingService;
import cn.bugstack.ai.es.EsClient;
import cn.bugstack.ai.rag.repository.KnowledgeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 混合检索：向量召回 + BM25 全文召回 + RRF 融合 → Top-K
 * 兜底：带分类无结果时放宽分类重试
 */
@Slf4j
@Service
public class HybridRetriever {

    private final KnowledgeRepository repository;
    private final EmbeddingService embeddingService;
    private final EsClient esClient;
    private final AiProperties properties;

    public HybridRetriever(KnowledgeRepository repository,
                           EmbeddingService embeddingService,
                           EsClient esClient,
                           AiProperties properties) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.esClient = esClient;
        this.properties = properties;
    }

    /** 关键词召回：ES 启用走 ES（BM25+IK），否则走 PG tsvector；ES 异常自动回退 PG */
    private List<ScoredChunk> fulltextSearch(String question, String tsQuery, String category, int topK) {
        if (esClient.enabled()) {
            try {
                esClient.ensureIndex();
                List<cn.bugstack.ai.es.EsClient.EsHit> hits = esClient.search(question, category, topK);
                if (!hits.isEmpty()) {
                    return hits.stream().map(hit -> ScoredChunk.builder()
                            .chunkId(hit.getChunkId())
                            .docId(hit.getDocId())
                            .docTitle(hit.getDocTitle())
                            .chunkText(hit.getChunkText())
                            .sectionPath(hit.getSectionPath())
                            .metadataJson(hit.getMetadataJson())
                            .score(hit.getScore())
                            .build()).collect(java.util.stream.Collectors.toList());
                }
                log.warn("ES 无结果/不可用，回退 PG tsvector query={}", question);
            } catch (Exception e) {
                log.warn("ES 检索异常，回退 PG tsvector: {}", e.getMessage());
            }
        }
        return tsQuery.isEmpty()
                ? Collections.emptyList()
                : repository.searchFulltext(tsQuery, category, topK);
    }

    /**
     * 父子 chunk 上下文展开：命中子块 → 替换为父块全文（去重、按分数排序）
     * 父块自身被命中则直接保留；子块命中但父块不存在则退回子块
     */
    public List<ScoredChunk> expandParents(List<ScoredChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return chunks;
        }
        java.util.Set<Long> parentIds = new java.util.HashSet<Long>();
        for (ScoredChunk chunk : chunks) {
            if (chunk.getParentId() != null) {
                parentIds.add(chunk.getParentId());
            }
        }
        java.util.Map<Long, String> parentTexts = repository.findParentTexts(parentIds);
        java.util.Map<Long, ScoredChunk> merged = new java.util.LinkedHashMap<Long, ScoredChunk>();
        for (ScoredChunk chunk : chunks) {
            boolean useParent = chunk.getParentId() != null && parentTexts.containsKey(chunk.getParentId());
            Long key = useParent ? chunk.getParentId() : chunk.getChunkId();
            ScoredChunk existing = merged.get(key);
            if (existing == null) {
                if (useParent) {
                    merged.put(key, ScoredChunk.builder()
                            .chunkId(chunk.getParentId())
                            .docId(chunk.getDocId())
                            .docTitle(chunk.getDocTitle())
                            .chunkText(parentTexts.get(chunk.getParentId()))
                            .sectionPath(chunk.getSectionPath())
                            .metadataJson(chunk.getMetadataJson())
                            .score(chunk.getScore())
                            .build());
                } else {
                    merged.put(key, chunk);
                }
            } else if (existing.getScore() < chunk.getScore()) {
                existing.setScore(chunk.getScore());
            }
        }
        List<ScoredChunk> result = new java.util.ArrayList<ScoredChunk>(merged.values());
        result.sort(java.util.Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        return result;
    }

    public List<ScoredChunk> retrieve(String question, String categoryFilter) {
        return retrieve(question, categoryFilter, properties.getRetrieval().getResultTopK());
    }

    /** 指定返回条数（重排场景：召回 Top-10 再精排） */
    public List<ScoredChunk> retrieve(String question, String categoryFilter, int topK) {
        AiProperties.Retrieval cfg = properties.getRetrieval();
        String embeddingLiteral = EmbeddingService.toVectorLiteral(embeddingService.embed(question));

        List<ScoredChunk> vector = repository.searchVector(
                embeddingLiteral, categoryFilter, cfg.getVectorTopK(), cfg.getSimilarityThreshold());
        String tsQuery = TextSearchUtil.toTsQuery(question);
        List<ScoredChunk> fulltext = fulltextSearch(question, tsQuery, categoryFilter, cfg.getFulltextTopK());

        List<ScoredChunk> fused = RrfFusion.fuse(vector, fulltext, cfg.getRrfK(), topK);

        if (fused.isEmpty() && categoryFilter != null) {
            vector = repository.searchVector(
                    embeddingLiteral, null, cfg.getVectorTopK(), cfg.getSimilarityThreshold());
            fulltext = fulltextSearch(question, tsQuery, null, cfg.getFulltextTopK());
            fused = RrfFusion.fuse(vector, fulltext, cfg.getRrfK(), topK);
        }
        return fused;
    }

}