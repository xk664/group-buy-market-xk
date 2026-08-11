package cn.bugstack.ai.rag.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResult {

    private int total;
    private int evaluated;
    private int hits;
    /** Top-5 命中率 = hits / evaluated */
    private double hitRate;
    /** Recall@5（当前单答案标注下与命中率一致） */
    private double recallAt5;
    private Map<String, CategoryStat> categoryStats;
    private List<String> misses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private int total;
        private int hits;
        private double hitRate;

        public static CategoryStat merge(List<CategoryStat> stats) {
            int total = stats.stream().mapToInt(CategoryStat::getTotal).sum();
            int hits = stats.stream().mapToInt(CategoryStat::getHits).sum();
            return CategoryStat.builder()
                    .total(total).hits(hits)
                    .hitRate(total == 0 ? 0 : (double) hits / total)
                    .build();
        }

        public static CategoryStat empty() {
            return CategoryStat.builder().total(0).hits(0).hitRate(0).build();
        }
    }

    public static EvalResult empty() {
        return EvalResult.builder()
                .total(0).evaluated(0).hits(0).hitRate(0).recallAt5(0)
                .categoryStats(new LinkedHashMap<>())
                .misses(new ArrayList<>())
                .build();
    }

}