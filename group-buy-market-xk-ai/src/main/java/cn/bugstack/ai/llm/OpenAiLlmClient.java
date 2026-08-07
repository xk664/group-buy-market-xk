package cn.bugstack.ai.llm;

import cn.bugstack.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容云端 LLM 客户端（DashScope compatible-mode / 任意 OpenAI 兼容服务）
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    private final AiProperties.Llm props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public OpenAiLlmClient(AiProperties.Llm props, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.props = props;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = props.getBaseUrl().replaceAll("/+$", "");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", props.getChatModel());
        body.put("temperature", 0.2);
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        JsonNode resp = post("/chat/completions", body);
        JsonNode choice = resp.path("choices").path(0);
        String content = choice.path("message").path("content").asText(null);
        if (content == null) {
            throw new IllegalStateException("LLM 响应缺少 choices[0].message.content: " + resp);
        }
        return content.trim();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", props.getEmbeddingModel());
        ArrayNode input = body.putArray("input");
        texts.forEach(input::add);

        JsonNode resp = post("/embeddings", body);
        JsonNode data = resp.path("data");
        List<float[]> result = new ArrayList<>(texts.size());
        for (JsonNode item : data) {
            JsonNode emb = item.path("embedding");
            float[] vector = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                vector[i] = (float) emb.get(i).asDouble();
            }
            result.add(vector);
        }
        return result;
    }

    @Override
    public boolean isMock() {
        return false;
    }

    private JsonNode post(String path, ObjectNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey().trim());
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    baseUrl + path, new HttpEntity<>(body.toString(), headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("LLM HTTP " + resp.getStatusCode() + ": " + resp.getBody());
            }
            return objectMapper.readTree(resp.getBody());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("调用 LLM 失败 path=" + path, e);
        }
    }

}