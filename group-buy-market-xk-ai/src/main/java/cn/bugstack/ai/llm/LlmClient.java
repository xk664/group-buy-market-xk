package cn.bugstack.ai.llm;

import java.util.List;

/**
 * LLM 能力抽象：聊天 + Embedding
 * 生产使用 OpenAI 兼容云端 API；本地无 Key 时使用 Mock 实现联调
 */
public interface LlmClient {

    /** 单轮对话，返回纯文本回答 */
    String chat(String systemPrompt, String userPrompt);

    /** 批量 Embedding，返回与输入一一对应的向量 */
    List<float[]> embed(List<String> texts);

    /** 当前是否 Mock 实现 */
    boolean isMock();

}