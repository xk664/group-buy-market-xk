package cn.bugstack.ai.rag;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.es.EsClient;
import cn.bugstack.ai.es.EsDoc;
import cn.bugstack.ai.rag.conflict.ConflictService;
import cn.bugstack.ai.rag.embedding.EmbeddingService;
import cn.bugstack.ai.rag.parser.MultiFormatDocumentParser;
import cn.bugstack.ai.rag.parser.ParsedDocument;
import cn.bugstack.ai.rag.repository.KnowledgeRepository;
import cn.bugstack.ai.rag.splitter.Chunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 知识入库管线（增量索引 + 冲突检测联动）：
 * - 文件指纹（SHA-256）比对：未变跳过、新增处理、变化重处理、删除下线
 * - 入库后自动冲突检测：有冲突 → CONFLICT 拦截；无冲突 → ACTIVE，并下线旧版本
 */
@Slf4j
@Service
public class KnowledgeIngestionService {

    private final MultiFormatDocumentParser parser;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final KnowledgeRepository repository;
    private final ConflictService conflictService;
    private final EsClient esClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public KnowledgeIngestionService(MultiFormatDocumentParser parser,
                                     EmbeddingService embeddingService,
                                     KnowledgeRepository repository,
                                     ConflictService conflictService,
                                     EsClient esClient,
                                     ObjectMapper objectMapper,
                                     AiProperties properties) {
        this.parser = parser;
        this.chunkingService = new ChunkingService(properties.getChunking());
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.conflictService = conflictService;
        this.esClient = esClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** 全量扫描式增量入库：返回统计（scanned/skipped/added/updated/deleted/conflicts） */
    public IngestResult ingest(String rootPath) {
        Path root = Paths.get(rootPath == null || rootPath.trim().isEmpty()
                ? properties.getKnowledge().getRootPath() : rootPath)
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("知识语料目录不存在: " + root);
        }
        List<Path> files;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            files = stream.filter(parser::isSupported).sorted().collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("遍历知识语料失败: " + root, e);
        }

        int scanned = 0;
        int skipped = 0;
        int added = 0;
        int updated = 0;
        int deleted = 0;
        int conflicts = 0;
        int chunkCount = 0;
        Set<String> seenPaths = new HashSet<String>();
        List<String> failures = new ArrayList<String>();

        for (Path file : files) {
            scanned++;
            String sourcePath = file.toAbsolutePath().normalize().toString();
            seenPaths.add(sourcePath);
            try {
                FileProcessResult result = processFile(file, sourcePath);
                if (result.skipped) {
                    skipped++;
                } else if (result.inserted) {
                    added++;
                    chunkCount += result.chunkCount;
                } else {
                    updated++;
                    chunkCount += result.chunkCount;
                }
                conflicts += result.conflicts;
            } catch (Exception e) {
                log.error("知识入库失败 path={}", file, e);
                failures.add(file + ": " + e.getMessage());
            }
        }

        // 删除检测：目录下已不存在的 ACTIVE 文档 → DISABLED
        List<KnowledgeRepository.DocEntity> docs = repository.listDocsBySourcePrefix(root.toString());
        for (KnowledgeRepository.DocEntity doc : docs) {
            if (!seenPaths.contains(doc.getSourcePath())) {
                repository.markDisabledBySourcePath(doc.getSourcePath());
                esClient.deleteBySourcePath(doc.getSourcePath());
                deleted++;
                log.info("知识文档已删除/下线 source={}", doc.getSourcePath());
            }
        }

        log.info("知识增量索引完成 scanned={} skipped={} added={} updated={} deleted={} conflicts={}",
                scanned, skipped, added, updated, deleted, conflicts);
        return new IngestResult(scanned, skipped, added, updated, deleted, conflicts, chunkCount, failures);
    }

    /** 单文件增量（目录监听 / admin 单文件接口用） */
    public FileProcessResult ingestFile(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalStateException("文件不存在: " + absolute);
        }
        return processFile(absolute, absolute.toString());
    }

    /** 是否支持该文件格式（目录监听过滤用） */
    public boolean parserSupport(Path file) {
        return parser.isSupported(file);
    }

    /** 文件删除处理（目录监听用）：仅下线，保留审计 */
    public void markDeleted(Path file) {
        String sourcePath = file.toAbsolutePath().normalize().toString();
        repository.markDisabledBySourcePath(sourcePath);
        esClient.deleteBySourcePath(sourcePath);
    }

    public int deactivateVersion(String category, String version) {
        return repository.deactivateVersion(category, version);
    }

    // ==================== 单文件处理 ====================

    private FileProcessResult processFile(Path file, String sourcePath) {
        // 1. 指纹比对：未变直接跳过
        String hash = FileHashUtil.sha256(file);
        KnowledgeRepository.DocEntity existing = repository.findBySourcePath(sourcePath);
        if (existing != null && hash.equals(existing.getContentHash())) {
            return FileProcessResult.skipped();
        }

        // 2. 解析 → 切分 → 向量化
        ParsedDocument parsed = parser.parse(file);
        ParsedDocument doc = normalize(parsed, file);
        List<Chunk> chunks = chunkingService.split(doc);
        if (chunks.isEmpty()) {
            log.warn("文档无有效内容，跳过入库 path={}", file);
            return FileProcessResult.skipped();
        }
        List<float[]> embeddings = embeddingService.embed(
                chunks.stream().map(Chunk::getText).collect(java.util.stream.Collectors.toList()));

        // 3. 先以 DRAFT 写入（避免入库瞬间被检索到）
        long docId = repository.upsertDoc(
                doc.getDocType(), doc.getTitle(), sourcePath,
                doc.getDocVersion(), doc.getEffectiveDate(), docMetadata(doc),
                hash, DocStatus.DRAFT.name());
        repository.deleteChunks(docId);

        List<KnowledgeRepository.ChunkEntity> entities = new ArrayList<KnowledgeRepository.ChunkEntity>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            entities.add(KnowledgeRepository.ChunkEntity.builder()
                    .chunkIndex(chunk.getIndex())
                    .chunkText(chunk.getText())
                    .searchText(TextSearchUtil.toSearchText(chunk.getText()))
                    .sectionPath(chunk.getSectionPath())
                    .embeddingLiteral(EmbeddingService.toVectorLiteral(embeddings.get(i)))
                    .metadataJson(chunkMetadata(doc, chunk))
                    .build());
        }
        // 父子 chunk：先插父块拿 id，子块再带 parent_id 插入
        java.util.Map<Integer, Long> chunkIdByIndex = new java.util.HashMap<Integer, Long>();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            Long parentId = chunk.getParentIndex() >= 0 ? chunkIdByIndex.get(chunk.getParentIndex()) : null;
            long chunkId = repository.insertChunk(docId, entities.get(i), parentId);
            chunkIdByIndex.put(chunk.getIndex(), chunkId);
        }

        // 3.5 ES 同步（启用时）：先删旧 chunk，再写新 chunk
        syncEs(docId, doc, sourcePath, chunks, chunkIdByIndex);

        // 4. 冲突检测：有冲突 → CONFLICT 拦截；无冲突 → ACTIVE + 旧版本下线
        int conflicts = conflictService.detectForDoc(docId);
        if (conflicts > 0) {
            repository.setDocStatus(docId, DocStatus.CONFLICT.name());
        } else {
            repository.setDocStatus(docId, DocStatus.ACTIVE.name());
            List<KnowledgeRepository.DocEntity> others = repository.findActiveByDocTypeAndTitle(
                    doc.getDocType(), doc.getTitle(), docId);
            for (KnowledgeRepository.DocEntity other : others) {
                if (other.getDocVersion() != null && doc.getDocVersion() != null
                        && !other.getDocVersion().equals(doc.getDocVersion())) {
                    repository.deactivateDoc(other.getId());
                    log.info("新版本 {} v{} 已生效，旧版本 docId={} 已下线",
                            doc.getTitle(), doc.getDocVersion(), other.getId());
                }
            }
        }
        log.info("知识入库完成 doc={} chunks={} conflicts={} path={}",
                doc.getTitle(), entities.size(), conflicts, file);
        return new FileProcessResult(existing == null, false, entities.size(), conflicts);
    }

    /** ES 同步：删除该文档旧 chunk 后写入新 chunk（ES 未启用时空转） */
    private void syncEs(long docId, ParsedDocument doc, String sourcePath,
                        List<Chunk> chunks, java.util.Map<Integer, Long> chunkIdByIndex) {
        if (!esClient.enabled()) {
            return;
        }
        try {
            esClient.ensureIndex();
            esClient.deleteByDoc(docId);
            for (Chunk chunk : chunks) {
                Long chunkId = chunkIdByIndex.get(chunk.getIndex());
                if (chunkId == null) {
                    continue;
                }
                esClient.indexChunk(EsDoc.builder()
                        .chunkId(chunkId)
                        .docId(docId)
                        .docTitle(doc.getTitle())
                        .chunkText(chunk.getText())
                        .sectionPath(chunk.getSectionPath())
                        .category(doc.getCategory())
                        .sourcePath(sourcePath)
                        .metadataJson(chunkMetadata(doc, chunk))
                        .build());
            }
            log.info("ES 同步完成 docId={} chunks={}", docId, chunks.size());
        } catch (Exception e) {
            log.warn("ES 同步失败（PG 仍可用）docId={}: {}", docId, e.getMessage());
        }
    }

    private ParsedDocument normalize(ParsedDocument parsed, Path file) {
        String category = parsed.getCategory();
        if (category == null || category.trim().isEmpty()) {
            String dir = file.getParent() == null ? "" : file.getParent().getFileName().toString();
            String dirKey = dir.toLowerCase(Locale.ROOT);
            if ("refund".equals(dirKey)) {
                category = "REFUND";
            } else if ("product".equals(dirKey)) {
                category = "PRODUCT";
            } else if ("groupon".equals(dirKey)) {
                category = "GROUPON";
            } else {
                category = "FAQ";
            }
        }
        String docType = parsed.getDocType();
        if (docType == null || docType.trim().isEmpty()) {
            docType = "FAQ".equals(category) ? "FAQ"
                    : parsed.getFileName().contains("参数表") ? "PARAM_TABLE" : "POLICY";
        }
        return ParsedDocument.builder()
                .sourcePath(parsed.getSourcePath())
                .fileName(parsed.getFileName())
                .title(parsed.getTitle())
                .category(category)
                .docType(docType)
                .docVersion(parsed.getDocVersion())
                .effectiveDate(parsed.getEffectiveDate())
                .productId(parsed.getProductId())
                .sourceUrl(parsed.getSourceUrl())
                .sections(parsed.getSections())
                .rawText(parsed.getRawText())
                .build();
    }

    private String docMetadata(ParsedDocument doc) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("category", doc.getCategory());
        meta.put("doc_type", doc.getDocType());
        meta.put("doc_version", doc.getDocVersion());
        meta.put("title", doc.getTitle());
        meta.put("effective_date", doc.getEffectiveDate());
        meta.put("product_id", doc.getProductId());
        meta.put("source", doc.getFileName());
        return toJson(meta);
    }

    private String chunkMetadata(ParsedDocument doc, Chunk chunk) {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("category", doc.getCategory());
        meta.put("doc_type", doc.getDocType());
        meta.put("doc_version", doc.getDocVersion());
        meta.put("title", doc.getTitle());
        meta.put("section_path", chunk.getSectionPath());
        meta.put("product_id", doc.getProductId());
        meta.put("source", doc.getFileName());
        return toJson(meta);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化元数据失败", e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class IngestResult {
        private int scanned;
        private int skipped;
        private int added;
        private int updated;
        private int deleted;
        private int conflicts;
        private int chunkCount;
        private List<String> failures;
    }

    @Data
    @AllArgsConstructor
    public static class FileProcessResult {
        private boolean inserted;
        private boolean skipped;
        private int chunkCount;
        private int conflicts;

        public static FileProcessResult skipped() {
            return new FileProcessResult(false, true, 0, 0);
        }
    }

}