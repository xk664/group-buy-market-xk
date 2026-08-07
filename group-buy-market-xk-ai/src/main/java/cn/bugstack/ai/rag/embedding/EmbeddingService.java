package cn.bugstack.ai.rag.embedding;

import cn.bugstack.ai.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Embedding 服务：批量调用 LLM 客户端，同文本保证同向量
 */
@Service
public class EmbeddingService {

    private static final int BATCH_SIZE = 16;

    private final LlmClient llmClient;

    public EmbeddingService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public float[] embed(String text) {
        return embed(Collections.singletonList(text)).get(0);
    }

    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(texts.size(), i + BATCH_SIZE));
            result.addAll(llmClient.embed(batch));
        }
        return result;
    }

    /** 将 float[] 转为 PG vector 字面量字符串，如 [0.1,0.2,...] */
    public static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

}