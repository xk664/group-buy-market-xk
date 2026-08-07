package cn.bugstack.ai.chat;

/**
 * 查询分析器：Phase1 规则版；Phase2 提供 LLM 版（ai.analyzer.mode=llm）
 */
public interface QueryAnalyzer {

    QueryPlan analyze(String userId, String question);

}