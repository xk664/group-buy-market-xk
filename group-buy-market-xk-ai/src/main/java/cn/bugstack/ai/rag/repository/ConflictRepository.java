package cn.bugstack.ai.rag.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * 知识冲突记录
 */
@Repository
public class ConflictRepository {

    private final JdbcTemplate aiJdbcTemplate;

    public ConflictRepository(JdbcTemplate aiJdbcTemplate) {
        this.aiJdbcTemplate = aiJdbcTemplate;
    }

    /** 插入冲突记录（chunk 对已规范化：chunk_a_id < chunk_b_id，重复自动忽略） */
    public void insert(long docAId, long chunkAId, long docBId, long chunkBId,
                       String topic, String reason, String severity) {
        aiJdbcTemplate.update(
                "INSERT INTO ai_conflict (doc_a_id, chunk_a_id, doc_b_id, chunk_b_id, topic, reason, severity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (chunk_a_id, chunk_b_id) DO NOTHING",
                docAId, chunkAId, docBId, chunkBId, topic, reason, severity);
    }

    public List<ConflictRecord> listByStatus(String status, int limit) {
        String sql = "SELECT cf.id, cf.doc_a_id, cf.chunk_a_id, cf.doc_b_id, cf.chunk_b_id, " +
                "cf.topic, cf.reason, cf.severity, cf.status, cf.created_at, " +
                "da.title AS title_a, db.title AS title_b " +
                "FROM ai_conflict cf " +
                "JOIN ai_knowledge_doc da ON da.id = cf.doc_a_id " +
                "JOIN ai_knowledge_doc db ON db.id = cf.doc_b_id " +
                "WHERE (? IS NULL OR cf.status = ?) " +
                "ORDER BY cf.created_at DESC LIMIT ?";
        return aiJdbcTemplate.query(sql, (rs, i) -> ConflictRecord.builder()
                        .id(rs.getLong("id"))
                        .docAId(rs.getLong("doc_a_id"))
                        .chunkAId(rs.getLong("chunk_a_id"))
                        .docBId(rs.getLong("doc_b_id"))
                        .chunkBId(rs.getLong("chunk_b_id"))
                        .topic(rs.getString("topic"))
                        .reason(rs.getString("reason"))
                        .severity(rs.getString("severity"))
                        .status(rs.getString("status"))
                        .createdAt(rs.getTimestamp("created_at"))
                        .titleA(rs.getString("title_a"))
                        .titleB(rs.getString("title_b"))
                        .build(),
                status, status, limit);
    }

    public int resolve(long id, String action) {
        return aiJdbcTemplate.update(
                "UPDATE ai_conflict SET status = ?, resolved_at = now() WHERE id = ? AND status = 'PENDING'",
                action, id);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictRecord {
        private long id;
        private long docAId;
        private long chunkAId;
        private long docBId;
        private long chunkBId;
        private String topic;
        private String reason;
        private String severity;
        private String status;
        private Timestamp createdAt;
        private String titleA;
        private String titleB;
    }

}