package cn.bugstack.ai.tool;

import cn.bugstack.ai.chat.QueryPlan;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具注册表：按顺序匹配第一个支持的客服工具
 */
@Component
public class ToolRegistry {

    private final List<CustomerServiceTool> tools;

    public ToolRegistry(List<CustomerServiceTool> tools) {
        this.tools = tools;
    }

    /** 返回第一个支持并执行成功的工具结果；无匹配返回 null */
    public ToolResult tryExecute(String question, QueryPlan plan) {
        for (CustomerServiceTool tool : tools) {
            if (tool.supports(question, plan)) {
                return tool.execute(question, plan);
            }
        }
        return null;
    }

    public List<CustomerServiceTool> all() {
        return tools;
    }

}