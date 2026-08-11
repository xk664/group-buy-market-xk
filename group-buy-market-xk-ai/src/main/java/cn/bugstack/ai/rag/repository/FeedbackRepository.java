package cn.bugstack.ai.rag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用户反馈（点赞/点踩/转人工）
 */
@Repository
public class FeedbackRepository {

    private final JdbcTemplate aiJdbcTemplate;

    public FeedbackRepository(JdbcTemplate aiJdbcTemplate) {
        this.aiJdbcTemplate = aiJdbcTemplate;
    }

    public void insert(String messageId, String userId, String feedbackType, String content) {
        aiJdbcTemplate.update(
                "INSERT INTO ai_feedback (message_id, user_id, feedback_type, content) VALUES (?, ?, ?, ?)",
                messageId, userId, feedbackType, content);
    }

}