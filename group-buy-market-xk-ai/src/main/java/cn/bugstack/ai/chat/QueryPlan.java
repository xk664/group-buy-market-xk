package cn.bugstack.ai.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 查询分析结果：意图 + 分类过滤 + 改写后的查询 + 是否需要实时工具
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPlan {

    private Intent intent;
    private String categoryFilter;
    private String rewrittenQuery;
    private boolean needTool;
    private Map<String, String> entities;

}