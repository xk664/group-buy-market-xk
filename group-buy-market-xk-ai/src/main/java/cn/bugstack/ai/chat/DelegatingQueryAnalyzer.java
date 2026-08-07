package cn.bugstack.ai.chat;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 查询分析统一入口：ai.analyzer.mode=llm 且非 Mock 时走 LLM 分析，否则走规则分析
 */
@Primary
@Component
public class DelegatingQueryAnalyzer implements QueryAnalyzer {

    private final RuleQueryAnalyzer rule;
    private final LlmQueryAnalyzer llm;
    private final AiProperties properties;

    public DelegatingQueryAnalyzer(RuleQueryAnalyzer rule,
                                   LlmClient llmClient,
                                   ObjectMapper objectMapper,
                                   AiProperties properties) {
        this.rule = rule;
        this.llm = new LlmQueryAnalyzer(llmClient, objectMapper, rule);
        this.properties = properties;
    }

    @Override
    public QueryPlan analyze(String userId, String question) {
        if ("llm".equals(properties.getAnalyzer().getMode()) && !llm.isMock()) {
            return llm.analyze(userId, question);
        }
        return rule.analyze(userId, question);
    }

}