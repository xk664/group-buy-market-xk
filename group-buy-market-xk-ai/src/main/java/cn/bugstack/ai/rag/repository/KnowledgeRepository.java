package cn.bugstack.ai.rag.repository;

import cn.bugstack.ai.rag.DocStatus;
import cn.bugstack.ai.rag.retriever.ScoredChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库仓库：文档/切块写入 + 向量/全文召回 + 增量索引与冲突检测支持
 */
@Repository
public class KnowledgeRepository {

    private final JdbcTemplate aiJdbcTemplate;

    public KnowledgeRepository(JdbcTemplate aiJdbcTemplate) {
        this.aiJdbcTemplate = aiJdbcTemplate;
    }

    // ==================== 文档写入/查询 ====================

    public long upsertDoc(String docType, String title, String sourcePath,
                          String docVersion, String effectiveDate, String metadataJson,
                          String contentHash, String status) {
        String sql = "INSERT INTO ai_knowledge_doc (doc_type, title, source_path, doc_version, effective_date, metadata, content_hash, status) " +
                "VALUES (?, ?, ?, ?, ?::date, ?::jsonb, ?, ?) " +
                "ON CONFLICT (doc_type, title, doc_version) DO UPDATE SET " +
                "source_path = EXCLUDED.source_path, effective_date = EXCLUDED.effective_date, " +
                "metadata = EXCLUDED.metadata, content_hash = EXCLUDED.content_hash, " +
                "status = EXCLUDED.status, updated_at = now() " +
                "RETURNING id";
        Long id = aiJdbcTemplate.queryForObject(sql, Long.class,
                docType, title, sourcePath,
                docVersion, effectiveDate == null ? null : effectiveDate,
                metadataJson == null ? "{}" : metadataJson,
                contentHash, status);
        return id == null ? -1L : id;
    }

    public DocEntity findBySourcePath(String sourcePath) {
        List<DocEntity> list = aiJdbcTemplate.query(
                "SELECT id, doc_type, title, doc_version, content_hash, status, source_path FROM ai_knowledge_doc WHERE source_path = ?",
                (rs, i) -> DocEntity.builder()
                        .id(rs.getLong("id"))
                        .docType(rs.getString("doc_type"))
                        .title(rs.getString("title"))
                        .docVersion(rs.getString("doc_version"))
                        .contentHash(rs.getString("content_hash"))
                        .status(rs.getString("status"))
                        .sourcePath(rs.getString("source_path"))
                        .build(),
                sourcePath);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<DocEntity> findActiveByDocTypeAndTitle(String docType, String title, long excludeDocId) {
        return aiJdbcTemplate.query(
                "SELECT id, doc_type, title, doc_version, content_hash, status, source_path FROM ai_knowledge_doc " +
                        "WHERE doc_type = ? AND title = ? AND id <> ? AND status = 'ACTIVE' " +
                        "AND (expire_date IS NULL OR expire_date > CURRENT_DATE)",
                (rs, i) -> DocEntity.builder()
                        .id(rs.getLong("id"))
                        .docType(rs.getString("doc_type"))
                        .title(rs.getString("title"))
                        .docVersion(rs.getString("doc_version"))
                        .contentHash(rs.getString("content_hash"))
                        .status(rs.getString("status"))
                        .sourcePath(rs.getString("source_path"))
                        .build(),
                docType, title, excludeDocId);
    }

    public void setDocStatus(long docId, String status) {
        aiJdbcTemplate.update("UPDATE ai_knowledge_doc SET status = ?, updated_at = now() WHERE id = ?",
                status, docId);
    }

    public void deactivateDoc(long docId) {
        aiJdbcTemplate.update(
                "UPDATE ai_knowledge_doc SET expire_date = CURRENT_DATE, updated_at = now() " +
                        "WHERE id = ? AND expire_date IS NULL", docId);
    }

    public void markDisabledBySourcePath(String sourcePath) {
        aiJdbcTemplate.update(
                "UPDATE ai_knowledge_doc SET status = ?, expire_date = CURRENT_DATE, updated_at = now() " +
                        "WHERE source_path = ? AND status <> ?",
                DocStatus.DISABLED.name(), sourcePath, DocStatus.DISABLED.name());
    }

    public List<Long> listActiveDocIds() {
        return aiJdbcTemplate.queryForList(
                "SELECT id FROM ai_knowledge_doc WHERE status = 'ACTIVE' " +
                        "AND (expire_date IS NULL OR expire_date > CURRENT_DATE)", Long.class);
    }

    public List<DocEntity> listDocsBySourcePrefix(String prefix) {
        return aiJdbcTemplate.query(
                "SELECT id, doc_type, title, doc_version, content_hash, status, source_path FROM ai_knowledge_doc WHERE source_path LIKE ?",
                (rs, i) -> DocEntity.builder()
                        .id(rs.getLong("id"))
                        .docType(rs.getString("doc_type"))
                        .title(rs.getString("title"))
                        .docVersion(rs.getString("doc_version"))
                        .contentHash(rs.getString("content_hash"))
                        .status(rs.getString("status"))
                        .sourcePath(rs.getString("source_path"))
                        .sourcePath(rs.getString("source_path"))
                        .build(),
                prefix + "%");
    }

    public int deactivateVersion(String category, String version) {
        return aiJdbcTemplate.update(
                "UPDATE ai_knowledge_doc SET expire_date = CURRENT_DATE " +
                        "WHERE doc_type = ? AND doc_version = ? AND expire_date IS NULL",
                category, version);
    }

    public long countDocs() {
        Long count = aiJdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_knowledge_doc", Long.class);
        return count == null ? 0L : count;
    }

    public long countChunks() {
        Long count = aiJdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_knowledge_chunk", Long.class);
        return count == null ? 0L : count;
    }

    // ==================== 切块写入 ====================

    public void deleteChunks(long docId) {
        aiJdbcTemplate.update("DELETE FROM ai_knowledge_chunk WHERE doc_id = ?", docId);
    }

    public void insertChunks(long docId, List<ChunkEntity> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        aiJdbcTemplate.batchUpdate(
                "INSERT INTO ai_knowledge_chunk (doc_id, chunk_index, chunk_text, search_text, section_path, embedding, metadata) " +
                        "VALUES (?, ?, ?, ?, ?, ?::vector, ?::jsonb)",
                chunks,
                200,
                (ps, chunk) -> {
                    ps.setLong(1, docId);
                    ps.setInt(2, chunk.getChunkIndex());
                    ps.setString(3, chunk.getChunkText());
                    ps.setString(4, chunk.getSearchText());
                    ps.setString(5, chunk.getSectionPath());
                    ps.setString(6, chunk.getEmbeddingLiteral());
                    ps.setString(7, chunk.getMetadataJson());
                });
    }

    /** 逐条插入 chunk，返回自增 id（父子 chunk 需要 parent_id） */
    public long insertChunk(long docId, ChunkEntity chunk, Long parentId) {
        String sql = "INSERT INTO ai_knowledge_chunk (doc_id, chunk_index, chunk_text, search_text, section_path, embedding, metadata, parent_id) " +
                "VALUES (?, ?, ?, ?, ?, ?::vector, ?::jsonb, ?) RETURNING id";
        Long id = aiJdbcTemplate.queryForObject(sql, Long.class,
                docId, chunk.getChunkIndex(), chunk.getChunkText(),
                chunk.getSearchText(), chunk.getSectionPath(),
                chunk.getEmbeddingLiteral(), chunk.getMetadataJson(), parentId);
        return id == null ? -1L : id;
    }

    /** 批量查询父块文本（上下文展开用） */
    public java.util.Map<Long, String> findParentTexts(java.util.Collection<Long> parentIds) {
        java.util.Map<Long, String> result = new java.util.HashMap<Long, String>();
        if (parentIds == null || parentIds.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(parentIds.size(), "?"));
        String sql = "SELECT id, chunk_text FROM ai_knowledge_chunk WHERE id IN (" + placeholders + ")";
        aiJdbcTemplate.query(sql, rs -> {
            while (rs.next()) {
                result.put(rs.getLong("id"), rs.getString("chunk_text"));
            }
            return result;
        }, parentIds.toArray(new Object[0]));
        return result;
    }

    private Long readParentId(java.sql.ResultSet rs) throws java.sql.SQLException {
        long v = rs.getLong("parent_id");
        return rs.wasNull() ? null : v;
    }

    public List<ScoredChunk> listChunksByDoc(long docId) {
        return aiJdbcTemplate.query(
                "SELECT c.id, c.doc_id, d.title, c.chunk_text, c.section_path, c.metadata, c.parent_id, 0 AS score " +
                        "FROM ai_knowledge_chunk c JOIN ai_knowledge_doc d ON d.id = c.doc_id " +
                        "WHERE c.doc_id = ? ORDER BY c.chunk_index",
                (rs, i) -> ScoredChunk.builder()
                        .chunkId(rs.getLong("id"))
                        .docId(rs.getLong("doc_id"))
                        .docTitle(rs.getString("title"))
                        .chunkText(rs.getString("chunk_text"))
                        .sectionPath(rs.getString("section_path"))
                        .metadataJson(rs.getString("metadata"))
                        .parentId(readParentId(rs))
                        .score(rs.getDouble("score"))
                        .build(),
                docId);
    }

    // ==================== 检索 ====================

    public List<ScoredChunk> searchVector(String embeddingLiteral, String category, int topK, double threshold) {
        String sql = "SELECT c.id, c.doc_id, d.title, c.chunk_text, c.section_path, c.metadata, c.parent_id, " +
                "1 - (c.embedding <=> ?::vector) AS score " +
                "FROM ai_knowledge_chunk c JOIN ai_knowledge_doc d ON d.id = c.doc_id " +
                "WHERE 1 - (c.embedding <=> ?::vector) >= ? " +
                "AND d.status = 'ACTIVE' " +
                "AND (? IS NULL OR c.metadata->>'category' = ?) " +
                "AND (d.effective_date IS NULL OR d.effective_date <= CURRENT_DATE) " +
                "AND (d.expire_date IS NULL OR d.expire_date > CURRENT_DATE) " +
                "ORDER BY 1 - (c.embedding <=> ?::vector) DESC LIMIT ?";
        return aiJdbcTemplate.query(sql, (rs, i) -> ScoredChunk.builder()
                        .chunkId(rs.getLong("id"))
                        .docId(rs.getLong("doc_id"))
                        .docTitle(rs.getString("title"))
                        .chunkText(rs.getString("chunk_text"))
                        .sectionPath(rs.getString("section_path"))
                        .metadataJson(rs.getString("metadata"))
                        .parentId(readParentId(rs))
                        .score(rs.getDouble("score"))
                        .build(),
                embeddingLiteral, embeddingLiteral, threshold, category, category, embeddingLiteral, topK);
    }

    public List<ScoredChunk> searchFulltext(String tsQuery, String category, int topK) {
        String sql = "SELECT c.id, c.doc_id, d.title, c.chunk_text, c.section_path, c.metadata, c.parent_id, " +
                "ts_rank_cd(to_tsvector('simple', c.search_text), to_tsquery('simple', ?)) AS score " +
                "FROM ai_knowledge_chunk c JOIN ai_knowledge_doc d ON d.id = c.doc_id " +
                "WHERE to_tsvector('simple', c.search_text) @@ to_tsquery('simple', ?) " +
                "AND d.status = 'ACTIVE' " +
                "AND (? IS NULL OR c.metadata->>'category' = ?) " +
                "AND (d.effective_date IS NULL OR d.effective_date <= CURRENT_DATE) " +
                "AND (d.expire_date IS NULL OR d.expire_date > CURRENT_DATE) " +
                "ORDER BY score DESC LIMIT ?";
        return aiJdbcTemplate.query(sql, (rs, i) -> ScoredChunk.builder()
                        .chunkId(rs.getLong("id"))
                        .docId(rs.getLong("doc_id"))
                        .docTitle(rs.getString("title"))
                        .chunkText(rs.getString("chunk_text"))
                        .sectionPath(rs.getString("section_path"))
                        .metadataJson(rs.getString("metadata"))
                        .parentId(readParentId(rs))
                        .score(rs.getDouble("score"))
                        .build(),
                tsQuery, tsQuery, category, category, topK);
    }

    /** 冲突检测用：找与某 chunk 相似的其他 ACTIVE chunk（排除同文档） */
    public List<ScoredChunk> searchSimilarChunks(String embeddingLiteral, long excludeDocId, double threshold, int topK) {
        String sql = "SELECT c.id, c.doc_id, d.title, c.chunk_text, c.section_path, c.metadata, c.parent_id, " +
                "1 - (c.embedding <=> ?::vector) AS score " +
                "FROM ai_knowledge_chunk c JOIN ai_knowledge_doc d ON d.id = c.doc_id " +
                "WHERE 1 - (c.embedding <=> ?::vector) >= ? " +
                "AND c.doc_id <> ? " +
                "AND d.status = 'ACTIVE' " +
                "ORDER BY 1 - (c.embedding <=> ?::vector) DESC LIMIT ?";
        return aiJdbcTemplate.query(sql, (rs, i) -> ScoredChunk.builder()
                        .chunkId(rs.getLong("id"))
                        .docId(rs.getLong("doc_id"))
                        .docTitle(rs.getString("title"))
                        .chunkText(rs.getString("chunk_text"))
                        .sectionPath(rs.getString("section_path"))
                        .metadataJson(rs.getString("metadata"))
                        .parentId(readParentId(rs))
                        .score(rs.getDouble("score"))
                        .build(),
                embeddingLiteral, embeddingLiteral, threshold, excludeDocId, embeddingLiteral, topK);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocEntity {
        private long id;
        private String docType;
        private String title;
        private String docVersion;
        private String contentHash;
        private String sourcePath;
    private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkEntity {
        private int chunkIndex;
        private String chunkText;
        private String searchText;
        private String sectionPath;
        private String embeddingLiteral;
        private String metadataJson;
    }

}