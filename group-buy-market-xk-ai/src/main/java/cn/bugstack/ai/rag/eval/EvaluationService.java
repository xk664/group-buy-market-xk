package cn.bugstack.ai.rag.eval;

import cn.bugstack.ai.rag.retriever.HybridRetriever;
import cn.bugstack.ai.rag.retriever.Reranker;
import cn.bugstack.ai.rag.retriever.ScoredChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 离线评估：对 Golden 集逐条检索，计算 Top-5 命中率 / Recall@5 / 分类明细
 * 命中判定：检索 Top-5 中任意 chunk 的文档或章节与标注匹配即算命中（决策 v2.2 #7）
 */
@Slf4j
@Service
public class EvaluationService {

    private final HybridRetriever hybridRetriever;
    private final Reranker reranker;
    private final GoldenSetLoader loader = new GoldenSetLoader();

    public EvaluationService(HybridRetriever hybridRetriever, Reranker reranker) {
        this.hybridRetriever = hybridRetriever;
        this.reranker = reranker;
    }

    public EvalResult evaluate(String goldenPath) {
        Path path = Paths.get(goldenPath == null || goldenPath.trim().isEmpty()
                ? "docs/ai-eval/golden-qa.md" : goldenPath);
        List<GoldenCase> cases = loader.load(path);
        if (cases.isEmpty()) {
            return EvalResult.empty();
        }

        Map<String, int[]> statCounters = new LinkedHashMap<>(); // category -> [total, hits]
        List<String> misses = new ArrayList<>();
        int evaluated = 0;
        int hits = 0;

        for (GoldenCase c : cases) {
            if ("OTHER".equalsIgnoreCase(c.getCategory())) {
                continue;
            }
            evaluated++;
            String category = c.getCategory().toUpperCase();
            // 召回 Top-10 → 重排 Top-5（与在线链路一致）
            List<ScoredChunk> candidates = hybridRetriever.retrieve(c.getQuestion(), category, 10);
            List<ScoredChunk> chunks = reranker.rerank(c.getQuestion(), candidates, 5);
            boolean hit = chunks.stream().anyMatch(chunk -> match(chunk, c));
            int[] counter = statCounters.computeIfAbsent(category, k -> new int[2]);
            counter[0]++;
            if (hit) {
                hits++;
                counter[1]++;
            } else {
                String topDocs = chunks.stream()
                        .map(ScoredChunk::getDocTitle)
                        .distinct()
                        .limit(3)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("(无检索结果)");
                misses.add(c.getId() + " " + c.getQuestion() + " -> top: " + topDocs);
            }
        }

        Map<String, EvalResult.CategoryStat> stats = new LinkedHashMap<>();
        statCounters.forEach((category, counter) -> stats.put(category, EvalResult.CategoryStat.builder()
                .total(counter[0])
                .hits(counter[1])
                .hitRate(counter[0] == 0 ? 0 : (double) counter[1] / counter[0])
                .build()));

        double hitRate = evaluated == 0 ? 0 : (double) hits / evaluated;
        return EvalResult.builder()
                .total(cases.size())
                .evaluated(evaluated)
                .hits(hits)
                .hitRate(hitRate)
                .recallAt5(hitRate)
                .categoryStats(stats)
                .misses(misses)
                .build();
    }

    private boolean match(ScoredChunk chunk, GoldenCase c) {
        String fileName = c.getExpectedDoc().substring(c.getExpectedDoc().lastIndexOf('/') + 1);
        String title = fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
        boolean docHit = (chunk.getDocTitle() != null && chunk.getDocTitle().equals(title))
                || (chunk.getMetadataJson() != null && chunk.getMetadataJson().contains(fileName));
        boolean sectionHit = chunk.getSectionPath() != null
                && c.getExpectedSection() != null
                && !c.getExpectedSection().isEmpty()
                && chunk.getSectionPath().contains(c.getExpectedSection());
        return docHit || sectionHit;
    }

}