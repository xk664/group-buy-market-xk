package cn.bugstack.ai.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

/**
 * MCP Client 装配：ai.tool.mcp-enabled=true 时启用
 * 通过 stdio 拉起 McpOrderToolsServer 子进程，走标准 MCP 协议调用动态数据工具
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ai.tool", name = "mcp-enabled", havingValue = "true")
public class McpClientConfig {

    @Bean
    public McpJsonMapper mcpJsonMapper() {
        return new JacksonMcpJsonMapper(JsonMapper.builder().build());
    }

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpSyncClient(McpJsonMapper mcpJsonMapper) {
        String javaBin = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "java";
        ServerParameters parameters = ServerParameters.builder(javaBin)
                .args("-cp", System.getProperty("java.class.path"),
                        "cn.bugstack.ai.mcp.McpOrderToolsServer")
                .build();
        McpSyncClient client = McpClient.sync(new StdioClientTransport(parameters, mcpJsonMapper))
                .clientInfo(new McpSchema.Implementation("group-buy-market-ai", "1.0.0"))
                .build();
        client.initialize();
        List<McpSchema.Tool> tools = client.listTools().tools();
        log.info("MCP Client 连接成功，可用工具: {}", tools.stream()
                .map(McpSchema.Tool::name)
                .collect(java.util.stream.Collectors.toList()));
        return client;
    }

}