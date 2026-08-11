package cn.bugstack.ai.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Server 主程序（stdio 传输）：
 * 暴露 order_query / groupon_progress 两个动态数据工具。
 * 由主应用的 McpSyncClient 以子进程方式拉起；独立进程可通过
 * java -cp <classpath> cn.bugstack.ai.mcp.McpOrderToolsServer 启动。
 * 当前返回模拟数据；真实接入时改为查询 MySQL 业务库 + userId 归属校验。
 */
@Slf4j
public class McpOrderToolsServer {

    public static void main(String[] args) throws Exception {
        McpJsonMapper mapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());

        McpServerFeatures.SyncToolSpecification orderTool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("order_query")
                        .description("根据订单号查询订单/退款状态")
                        .inputSchema(mapper, "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\",\"description\":\"订单号\"}},\"required\":[\"orderId\"]}")
                        .build())
                .callHandler((exchange, request) -> {
                    Map<String, Object> toolArgs = request.arguments() == null ? new HashMap<String, Object>() : request.arguments();
                    Object orderId = toolArgs.get("orderId");
                    return McpSchema.CallToolResult.builder()
                            .addTextContent("订单 " + (orderId == null ? "未知" : orderId)
                                    + " 当前状态：退款审核中（MCP 模拟数据，真实接入 MySQL 后替换）")
                            .build();
                })
                .build();

        McpServerFeatures.SyncToolSpecification grouponTool = McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("groupon_progress")
                        .description("查询当前拼团进度（剩余人数/倒计时）")
                        .inputSchema(mapper, "{\"type\":\"object\",\"properties\":{}}")
                        .build())
                .callHandler((exchange, request) -> McpSchema.CallToolResult.builder()
                        .addTextContent("当前团 2/3 人，还差 1 人成团（MCP 模拟数据，真实接入拼团表后替换）")
                        .build())
                .build();

        McpSyncServer server = McpServer.sync(new StdioServerTransportProvider(mapper))
                .serverInfo(new McpSchema.Implementation("group-buy-market-mcp-server", "1.0.0"))
                .tools(orderTool, grouponTool)
                .build();

        log.info("MCP Order Server 启动完成，等待客户端连接...");
        // 阻塞保持进程存活
        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }

}