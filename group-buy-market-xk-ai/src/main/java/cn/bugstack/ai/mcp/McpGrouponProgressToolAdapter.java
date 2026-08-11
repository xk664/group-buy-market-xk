package cn.bugstack.ai.mcp;

import cn.bugstack.ai.chat.QueryPlan;
import cn.bugstack.ai.tool.CustomerServiceTool;
import cn.bugstack.ai.tool.ToolResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 拼团进度查询：通过 MCP 协议调用（ai.tool.mcp-enabled=true 时生效）
 */
@Component
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "true")
public class McpGrouponProgressToolAdapter implements CustomerServiceTool {

    private final McpToolCaller mcpToolCaller;

    public McpGrouponProgressToolAdapter(McpToolCaller mcpToolCaller) {
        this.mcpToolCaller = mcpToolCaller;
    }

    @Override
    public String name() {
        return "mcp_groupon_progress";
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
        return mcpToolCaller.callGrouponProgress();
    }

}