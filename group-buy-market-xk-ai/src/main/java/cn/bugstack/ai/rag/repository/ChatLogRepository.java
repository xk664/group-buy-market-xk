package cn.bugstack.ai.rag.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 会话日志（全链路可观测）
 */
@Repository
public class ChatLogRepository {

    private final JdbcTemplate aiJdbcTemplate;
    private final ObjectMapper objectMapper;

    public ChatLogRepository(JdbcTemplate aiJdbcTemplate, ObjectMapper objectMapper) {
        this.aiJdbcTemplate = aiJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void insert(String messageId, String userId, String sessionId,
                       String question, String intent, boolean needHuman,
                       String answer, List<Map<String, String>> references, long latencyMs) {
        String refsJson = "[]";
        if (references != null) {
            try {
                refsJson = objectMapper.writeValueAsString(references);
            } catch (Exception ignored) {
            }
        }
        aiJdbcTemplate.update(
                "INSERT INTO ai_chat_log (message_id, user_id, session_id, question, intent, need_human, answer, references_json, latency_ms) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)",
                messageId, userId, sessionId, question, intent, needHuman, answer, refsJson, latencyMs);
    }

    /** 未命中/转人工问题（反馈闭环：用于补知识） */
    public List<String> listNeedHumanQuestions(int limit) {
        return aiJdbcTemplate.query(
                "SELECT question FROM ai_chat_log WHERE need_human = TRUE AND question IS NOT NULL " +
                        "ORDER BY created_at DESC LIMIT ?",
                (rs, i) -> rs.getString("question"), limit);
    }

}