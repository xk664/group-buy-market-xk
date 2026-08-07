package cn.bugstack.ai.tool;

import cn.bugstack.ai.chat.QueryPlan;

/**
 * 客服工具 SPI：状态类问题（订单/退款进度/拼团进度）的实时数据查询
 */
public interface CustomerServiceTool {

    String name();

    boolean supports(String question, QueryPlan plan);

    ToolResult execute(String question, QueryPlan plan);

}