package cn.bugstack.ai.mcp;

import cn.bugstack.ai.chat.QueryPlan;
import cn.bugstack.ai.tool.CustomerServiceTool;
import cn.bugstack.ai.tool.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单/退款状态查询：通过 MCP 协议调用（ai.tool.mcp-enabled=true 时生效）
 */
@Component
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "true")
public class McpOrderQueryToolAdapter implements CustomerServiceTool {

    private static final Pattern ORDER_ID = Pattern.compile("\\d{6,}");

    private final McpToolCaller mcpToolCaller;

    public McpOrderQueryToolAdapter(McpToolCaller mcpToolCaller) {
        this.mcpToolCaller = mcpToolCaller;
    }

    @Override
    public String name() {
        return "mcp_order_query";
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
        String orderId = null;
        if (question != null) {
            Matcher matcher = ORDER_ID.matcher(question);
            if (matcher.find()) {
                orderId = matcher.group();
            }
        }
        return mcpToolCaller.callOrderQuery(orderId);
    }

}