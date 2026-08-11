package cn.bugstack.ai.es;

import cn.bugstack.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Elasticsearch 轻量客户端（REST 直连，无重依赖）：
 * 索引管理 / 文档写入 / 按 doc 删除 / 关键词搜索（标准 BM25 + 中文分词）
 * ai.es.enabled=false 时所有方法空转，不产生任何请求
 */
@Slf4j
@Component
public class EsClient {

    private final AiProperties.Es props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public EsClient(AiProperties properties, RestTemplate aiRestTemplate, ObjectMapper objectMapper) {
        this.props = properties.getEs();
        this.restTemplate = aiRestTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = props.getHost().replaceAll("/+$", "");
    }

    public boolean enabled() {
        return Boolean.TRUE.equals(props.getEnabled());
    }

    /** 确保索引存在（含 IK 分词 mapping；IK 不可用时自动回退 standard） */
    public synchronized void ensureIndex() {
        if (!enabled()) {
            return;
        }
        try {
            ResponseEntity<String> exists = restTemplate.exchange(
                    baseUrl + "/" + props.getIndex(), HttpMethod.HEAD,
                    new HttpEntity<Void>(headers()), String.class);
            if (exists.getStatusCode().is2xxSuccessful()) {
                return;
            }
        } catch (Exception e) {
            log.info("ES 索引不存在或不可达，尝试创建: {}", e.getMessage());
        }
        try {
            createIndex(props.getAnalyzer());
            log.info("ES 索引创建成功 index={} analyzer={}", props.getIndex(), props.getAnalyzer());
        } catch (Exception e) {
            log.warn("IK 分词器创建索引失败，回退 standard: {}", e.getMessage());
            try {
                createIndex("standard");
                log.info("ES 索引创建成功 index={} analyzer=standard", props.getIndex());
            } catch (Exception ex) {
                throw new IllegalStateException("ES 索引创建失败: " + ex.getMessage(), ex);
            }
        }
    }

    /** 索引一个 chunk */
    public void indexChunk(EsDoc doc) {
        if (!enabled()) {
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("chunk_id", doc.getChunkId());
            body.put("doc_id", doc.getDocId());
            body.put("doc_title", doc.getDocTitle());
            body.put("chunk_text", doc.getChunkText());
            body.put("section_path", doc.getSectionPath());
            body.put("category", doc.getCategory());
            body.put("source_path", doc.getSourcePath());
            body.put("metadata", objectMapper.readTree(doc.getMetadataJson()));
            restTemplate.exchange(
                    baseUrl + "/" + props.getIndex() + "/_doc/" + doc.getChunkId(),
                    HttpMethod.PUT, new HttpEntity<String>(body.toString(), headers()), String.class);
        } catch (Exception e) {
            log.error("ES 写入失败 chunkId={}", doc.getChunkId(), e);
        }
    }

    /** 删除某文档的全部 chunk */
    public void deleteByDoc(long docId) {
        if (!enabled()) {
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.putObject("query").putObject("term").put("doc_id", docId);
            restTemplate.exchange(
                    baseUrl + "/" + props.getIndex() + "/_delete_by_query?refresh=true",
                    HttpMethod.POST, new HttpEntity<String>(body.toString(), headers()), String.class);
        } catch (Exception e) {
            log.error("ES 按 doc 删除失败 docId={}", docId, e);
        }
    }

    /** 删除某源文件的全部 chunk（文件下线时） */
    public void deleteBySourcePath(String sourcePath) {
        if (!enabled()) {
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.putObject("query").putObject("term").put("source_path", sourcePath);
            restTemplate.exchange(
                    baseUrl + "/" + props.getIndex() + "/_delete_by_query?refresh=true",
                    HttpMethod.POST, new HttpEntity<String>(body.toString(), headers()), String.class);
        } catch (Exception e) {
            log.error("ES 按 source_path 删除失败 path={}", sourcePath, e);
        }
    }

    /**
     * 关键词检索：标准 BM25 + 中文分词（match），分类过滤
     * 返回结果按 _score 降序，最多 topK 条
     */
    public List<EsHit> search(String query, String category, int topK) {
        if (!enabled()) {
            return new ArrayList<EsHit>();
        }
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode bool = body.putObject("query").putObject("bool");
        ObjectNode match = bool.putObject("must").putObject("match");
        match.put("chunk_text", query);
        if (category != null) {
            bool.putObject("filter").putObject("term").put("category", category);
        }
        body.put("size", topK);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/" + props.getIndex() + "/_search",
                    HttpMethod.POST, new HttpEntity<String>(body.toString(), headers()), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("ES 搜索失败 status={} body={}", resp.getStatusCode(), resp.getBody());
                return new ArrayList<EsHit>();
            }
            return parseHits(objectMapper.readTree(resp.getBody()));
        } catch (Exception e) {
            log.warn("ES 搜索异常，回退 PG tsvector: {}", e.getMessage());
            return new ArrayList<EsHit>();
        }
    }

    private List<EsHit> parseHits(JsonNode root) {
        List<EsHit> hits = new ArrayList<EsHit>();
        JsonNode array = root.path("hits").path("hits");
        for (JsonNode hit : array) {
            JsonNode src = hit.path("_source");
            hits.add(EsHit.builder()
                    .chunkId(src.path("chunk_id").asLong())
                    .docId(src.path("doc_id").asLong())
                    .docTitle(src.path("doc_title").asText())
                    .chunkText(src.path("chunk_text").asText())
                    .sectionPath(src.path("section_path").asText())
                    .metadataJson(src.path("metadata").toString())
                    .score(hit.path("_score").asDouble())
                    .build());
        }
        return hits;
    }

    private void createIndex(String analyzer) {
        ObjectNode settings = objectMapper.createObjectNode();
        ObjectNode propsNode = settings.putObject("mappings").putObject("properties");
        propsNode.putObject("chunk_id").put("type", "long");
        propsNode.putObject("doc_id").put("type", "long");
        propsNode.putObject("doc_title").put("type", "keyword");
        propsNode.putObject("section_path").put("type", "keyword");
        propsNode.putObject("category").put("type", "keyword");
        propsNode.putObject("source_path").put("type", "keyword");
        propsNode.putObject("metadata").put("enabled", false);
        ObjectNode textNode = propsNode.putObject("chunk_text");
        textNode.put("type", "text");
        textNode.put("analyzer", analyzer);
        textNode.put("search_analyzer", analyzer);

        restTemplate.exchange(
                baseUrl + "/" + props.getIndex(),
                HttpMethod.PUT, new HttpEntity<String>(settings.toString(), headers()), String.class);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (props.getUsername() != null && !props.getUsername().isEmpty()) {
            String token = Base64.getEncoder().encodeToString(
                    (props.getUsername() + ":" + (props.getPassword() == null ? "" : props.getPassword()))
                            .getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
        return headers;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EsHit {
        private long chunkId;
        private long docId;
        private String docTitle;
        private String chunkText;
        private String sectionPath;
        private String metadataJson;
        private double score;
    }

}