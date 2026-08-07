package cn.bugstack.ai.tool;

import cn.bugstack.ai.chat.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 拼团进度查询工具
 * 当前为模拟实现；真实接入：查询拼团 team 表 + 归属校验（Phase3.5）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "false", matchIfMissing = true)
public class GrouponProgressTool implements CustomerServiceTool {

    @Override
    public String name() {
        return "groupon_progress";
    }

    @Override
    public boolean supports(String question, QueryPlan plan) {
        if (!plan.isNeedTool()) {
            return false;
        }
        String q = question == null ? "" : question;
        return (q.contains("拼团") || q.contains("团")) && (q.contains("进度") || q.contains("差几") || q.contains("还剩") || q.contains("几人"));
    }

    @Override
    public ToolResult execute(String question, QueryPlan plan) {
        log.warn("GrouponProgressTool 当前为模拟实现；真实接入见 Phase3.5（拼团 team 表 + 归属校验）");
        return ToolResult.builder()
                .success(true)
                .data("当前团 2/3 人，还差 1 人成团（模拟数据，真实接入后替换）")
                .message("ok")
                .build();
    }

}