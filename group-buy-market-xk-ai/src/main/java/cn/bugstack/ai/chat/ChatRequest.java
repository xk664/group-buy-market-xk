package cn.bugstack.ai.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 客服对话请求（单轮）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String userId;
    private String question;
    /** 仅用于日志关联，不参与多轮上下文 */
    private String sessionId;

}