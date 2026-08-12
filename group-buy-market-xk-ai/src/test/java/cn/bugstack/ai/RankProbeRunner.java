package cn.bugstack.ai;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.rag.eval.GoldenCase;
import cn.bugstack.ai.rag.eval.GoldenSetLoader;
import cn.bugstack.ai.rag.retriever.HybridRetriever;
import cn.bugstack.ai.rag.retriever.Reranker;
import cn.bugstack.ai.rag.retriever.ScoredChunk;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 排名探测：对 Golden 每题输出正确 chunk 在 Top-10 中的位置
 * - scoreRank:   按 RRF/相似度分数排序后的 rank（1..10 或 MISS）
 * - rerankRank:  rerank-mode=cross-encoder 时额外输出重排后的 rank
 * 用法：java ... cn.bugstack.ai.RankProbeRunner <goldenPath> [--ai.xxx]
 */
@SpringBootApplication(scanBasePackages = "cn.bugstack.ai")
public class RankProbeRunner {

    public static void main(String[] args) {
        java.util.List<String> springArgs = new ArrayList<String>();
        String pathArg = null;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                springArgs.add(arg);
            } else if (pathArg == null) {
                pathArg = arg;
            }
        }
        SpringApplication app = new SpringApplication(RankProbeRunner.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(springArgs.toArray(new String[0]));
        try {
            HybridRetriever retriever = ctx.getBean(HybridRetriever.class);
            Reranker reranker = ctx.getBean(Reranker.class);
            AiProperties props = ctx.getBean(AiProperties.class);
            List<GoldenCase> cases = new GoldenSetLoader().load(Paths.get(
                    pathArg == null ? "docs/ai-eval/golden-qa.md" : pathArg));
            boolean cross = "cross-encoder".equals(props.getAnalyzer().getRerankMode());

            int[] scoreBuckets = new int[12]; // 1..10, 11=miss
            int[] rerankBuckets = new int[12];
            int evalCount = 0;
            for (GoldenCase c : cases) {
                if ("OTHER".equalsIgnoreCase(c.getCategory())) {
                    continue;
                }
                evalCount++;
                List<ScoredChunk> candidates = retriever.retrieve(c.getQuestion(), c.getCategory(), 10);
                int scoreRank = rankOf(candidates, c);
                scoreBuckets[scoreRank == -1 ? 11 : scoreRank]++;

                int rerankRank = -1;
                if (cross) {
                    List<ScoredChunk> reranked = reranker.rerank(c.getQuestion(), candidates, 10);
                    rerankRank = rankOf(reranked, c);
                    rerankBuckets[rerankRank == -1 ? 11 : rerankRank]++;
                }
                String topDocs = candidates.stream()
                        .map(ScoredChunk::getDocTitle)
                        .distinct()
                        .limit(3)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("(无)");
                System.out.println("PROBE " + c.getId() + " | " + c.getCategory() + " | scoreRank=" + scoreRank
                        + (cross ? " | rerankRank=" + rerankRank : "")
                        + " | " + c.getQuestion() + " | top: " + topDocs);
            }
            StringBuilder sb = new StringBuilder("SUMMARY evaluated=" + evalCount + " scoreBuckets[");
            for (int i = 1; i <= 11; i++) {
                sb.append(i == 11 ? "miss" : String.valueOf(i)).append("=").append(scoreBuckets[i]);
                if (i < 11) sb.append(",");
            }
            sb.append("]");
            if (cross) {
                sb.append(" rerankBuckets[");
                for (int i = 1; i <= 11; i++) {
                    sb.append(i == 11 ? "miss" : String.valueOf(i)).append("=").append(rerankBuckets[i]);
                    if (i < 11) sb.append(",");
                }
                sb.append("]");
            }
            System.out.println("PROBE_SUMMARY " + sb);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ctx.close();
        }
    }

    private static int rankOf(List<ScoredChunk> chunks, GoldenCase c) {
        List<ScoredChunk> sorted = new ArrayList<ScoredChunk>(chunks);
        sorted.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        for (int i = 0; i < sorted.size(); i++) {
            if (match(sorted.get(i), c)) {
                return i + 1;
            }
        }
        return -1;
    }

    private static boolean match(ScoredChunk chunk, GoldenCase c) {
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