package cn.bugstack.ai.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 结构化回答
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredAnswer {

    private String answer;
    private boolean needHuman;

}