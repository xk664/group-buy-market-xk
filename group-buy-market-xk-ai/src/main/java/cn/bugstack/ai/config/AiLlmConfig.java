package cn.bugstack.ai.config;

import cn.bugstack.ai.llm.LlmClient;
import cn.bugstack.ai.llm.MockLlmClient;
import cn.bugstack.ai.llm.OpenAiLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * LLM 客户端装配：API Key 为空或仍为占位符时自动使用 Mock 实现
 */
@Configuration
public class AiLlmConfig {

    private static final String PLACEHOLDER = "sk-<你的DashScope密钥>";

    @Bean
    public LlmClient llmClient(AiProperties aiProperties, RestTemplate aiRestTemplate, ObjectMapper objectMapper) {
        String apiKey = aiProperties.getLlm().getApiKey();
        boolean useMock = apiKey == null || apiKey.trim().isEmpty() || PLACEHOLDER.equals(apiKey.trim());
        if (useMock) {
            return new MockLlmClient();
        }
        return new OpenAiLlmClient(aiProperties.getLlm(), aiRestTemplate, objectMapper);
    }

}