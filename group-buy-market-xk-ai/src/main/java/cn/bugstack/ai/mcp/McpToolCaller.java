package cn.bugstack.ai.mcp;

import cn.bugstack.ai.tool.ToolResult;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 工具调用器：把动态数据工具调用封装为 ToolResult（与现有 CustomerServiceTool SPI 对接）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "true")
public class McpToolCaller {

    private final McpSyncClient mcpSyncClient;

    public McpToolCaller(McpSyncClient mcpSyncClient) {
        this.mcpSyncClient = mcpSyncClient;
    }

    public ToolResult callOrderQuery(String orderId) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("orderId", orderId == null ? "" : orderId);
        return call("order_query", args);
    }

    public ToolResult callGrouponProgress() {
        return call("groupon_progress", new HashMap<String, Object>());
    }

    private ToolResult call(String name, Map<String, Object> args) {
        try {
            McpSchema.CallToolResult result = mcpSyncClient.callTool(
                    McpSchema.CallToolRequest.builder().name(name).arguments(args).build());
            StringBuilder sb = new StringBuilder();
            if (result.content() != null) {
                for (McpSchema.Content content : result.content()) {
                    if (content instanceof McpSchema.TextContent) {
                        sb.append(((McpSchema.TextContent) content).text()).append('\n');
                    }
                }
            }
            boolean success = !Boolean.TRUE.equals(result.isError());
            return ToolResult.builder()
                    .success(success)
                    .data(sb.toString().trim())
                    .message(success ? "ok" : "mcp_tool_error")
                    .build();
        } catch (Exception e) {
            log.error("MCP 工具调用失败 name={}", name, e);
            return ToolResult.builder().success(false).message(e.getMessage()).build();
        }
    }

}