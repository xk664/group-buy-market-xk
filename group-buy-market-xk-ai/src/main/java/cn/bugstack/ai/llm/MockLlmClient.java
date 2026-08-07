package cn.bugstack.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock LLM 实现：用于未配置 API Key 时的本地联调
 * - Embedding：确定性哈希向量（1024 维，前 64 维有值），保证同文本同向量
 * - Chat：返回占位回答，明确提示未配置真实 Key
 */
@Slf4j
public class MockLlmClient implements LlmClient {

    private static final int DIM = 1024;

    public MockLlmClient() {
        log.warn("AI_LLM_API_KEY 未配置，使用 Mock LLM（仅本地联调，检索指标依赖 BM25 兜底）");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return "【Mock 模式】已检索到相关资料，当前未配置真实 API Key，返回占位回答。"
                + "配置 AI_LLM_API_KEY 后由真实模型生成答案。";
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embedOne(text));
        }
        return result;
    }

    private float[] embedOne(String text) {
        long seed = text == null ? 0L : (long) text.hashCode() * 31L + 7L;
        Random random = new Random(seed);
        float[] vector = new float[DIM];
        for (int i = 0; i < 64; i++) {
            vector[i] = random.nextFloat() * 2f - 1f;
        }
        return vector;
    }

    @Override
    public boolean isMock() {
        return true;
    }

}