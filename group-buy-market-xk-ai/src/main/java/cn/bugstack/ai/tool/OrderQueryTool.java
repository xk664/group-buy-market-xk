package cn.bugstack.ai.tool;

import cn.bugstack.ai.chat.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单/退款状态查询工具
 * 当前为模拟实现；真实接入：查询 MySQL 订单表并校验 user_id == 当前 userId（Phase3.5）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "false", matchIfMissing = true)
public class OrderQueryTool implements CustomerServiceTool {

    private static final Pattern ORDER_ID = Pattern.compile("\\d{6,}");

    @Override
    public String name() {
        return "order_query";
    }

    @Override
    public boolean supports(String question, QueryPlan plan) {
        if (!plan.isNeedTool()) {
            return false;
        }
        String q = question == null ? "" : question;
        return q.contains("订单") || (q.contains("退款") && q.contains("我"));
    }

    @Override
    public ToolResult execute(String question, QueryPlan plan) {
        log.warn("OrderQueryTool 当前为模拟实现；真实接入见 Phase3.5（MySQL 订单表 + 归属校验）");
        String orderId = extractOrderId(question);
        return ToolResult.builder()
                .success(true)
                .data("订单 " + (orderId == null ? "未知" : orderId) + " 当前状态：退款审核中（模拟数据，真实接入后替换）")
                .message("ok")
                .build();
    }

    private String extractOrderId(String question) {
        if (question == null) {
            return null;
        }
        Matcher matcher = ORDER_ID.matcher(question);
        return matcher.find() ? matcher.group() : null;
    }

}