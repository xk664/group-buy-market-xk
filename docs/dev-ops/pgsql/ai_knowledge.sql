-- ============================================================
-- AI 客服 RAG 知识库 DDL（PostgreSQL 12+）
-- 需启用 vector 扩展；zhparser 可选（无则用 search_text 空格分词方案）
-- 执行：psql -U postgres -d group_buy_market_ai -f ai_knowledge.sql
-- ============================================================
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
    id             BIGSERIAL PRIMARY KEY,
    doc_type       VARCHAR(32)  NOT NULL,           -- POLICY / PARAM_TABLE / PRODUCT_DETAIL / RULE / FAQ
    title          VARCHAR(255) NOT NULL,
    source_path    VARCHAR(512),
    source_url     VARCHAR(512),
    doc_version    VARCHAR(32),
    effective_date DATE,
    expire_date    DATE,
    metadata       JSONB,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (doc_type, title, doc_version)
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id           BIGSERIAL PRIMARY KEY,
    doc_id       BIGINT NOT NULL REFERENCES ai_knowledge_doc(id) ON DELETE CASCADE,
    chunk_index  INT NOT NULL,
    chunk_text   TEXT NOT NULL,
    search_text  TEXT NOT NULL DEFAULT '',          -- 中文按字符空格分词后的文本，用于 simple 配置全文检索
    section_path TEXT,
    embedding    vector(1024),                      -- 按实际 Embedding 维度（text-embedding-v3=1024）
    metadata     JSONB,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (doc_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON ai_knowledge_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_chunk_metadata  ON ai_knowledge_chunk USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_chunk_search    ON ai_knowledge_chunk USING gin (to_tsvector('simple', search_text));
CREATE INDEX IF NOT EXISTS idx_chunk_doc       ON ai_knowledge_chunk (doc_id);

-- 会话日志（全链路可观测）
CREATE TABLE IF NOT EXISTS ai_chat_log (
    id             BIGSERIAL PRIMARY KEY,
    message_id     VARCHAR(64) NOT NULL,
    user_id        VARCHAR(64),
    session_id     VARCHAR(64),
    question       TEXT,
    intent         VARCHAR(32),
    need_human     BOOLEAN NOT NULL DEFAULT FALSE,
    answer         TEXT,
    references_json JSONB,
    latency_ms     INT,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_chat_log_message ON ai_chat_log (message_id);
CREATE INDEX IF NOT EXISTS idx_chat_log_user    ON ai_chat_log (user_id, created_at);

-- 用户反馈（点赞/点踩/转人工）
CREATE TABLE IF NOT EXISTS ai_feedback (
    id            BIGSERIAL PRIMARY KEY,
    message_id    VARCHAR(64) NOT NULL,
    user_id       VARCHAR(64),
    feedback_type VARCHAR(16) NOT NULL,             -- LIKE / DISLIKE / HUMAN
    content       VARCHAR(512),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_feedback_message ON ai_feedback (message_id);

-- 可选：zhparser 中文分词（系统需安装 zhparser 扩展）
-- CREATE EXTENSION IF NOT EXISTS zhparser;
-- CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
-- ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;
-- ============================================================
-- 增量索引 + 冲突检测（v2 追加）
-- ============================================================
ALTER TABLE ai_knowledge_doc ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);
ALTER TABLE ai_knowledge_doc ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_doc_source ON ai_knowledge_doc (source_path);

-- 知识冲突记录
CREATE TABLE IF NOT EXISTS ai_conflict (
    id         BIGSERIAL PRIMARY KEY,
    doc_a_id   BIGINT NOT NULL,
    chunk_a_id BIGINT NOT NULL,
    doc_b_id   BIGINT NOT NULL,
    chunk_b_id BIGINT NOT NULL,
    topic      VARCHAR(128),
    reason     TEXT,
    severity   VARCHAR(16) NOT NULL DEFAULT 'HIGH',
    status     VARCHAR(16) NOT NULL DEFAULT 'PENDING',   -- PENDING/RESOLVED/IGNORED
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    UNIQUE (chunk_a_id, chunk_b_id)
);
CREATE INDEX IF NOT EXISTS idx_conflict_status ON ai_conflict (status);
CREATE INDEX IF NOT EXISTS idx_conflict_doc_a ON ai_conflict (doc_a_id);
-- ============================================================
-- 父子 chunk（v2 追加）：子块 parent_id 指向父块 id（自引用）
-- ============================================================
ALTER TABLE ai_knowledge_chunk ADD COLUMN IF NOT EXISTS parent_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_chunk_parent ON ai_knowledge_chunk (parent_id);