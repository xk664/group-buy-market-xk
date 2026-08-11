package cn.bugstack.ai.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈上报请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    private String messageId;
    private String userId;
    /** LIKE / DISLIKE / HUMAN */
    private String feedbackType;
    private String content;

}