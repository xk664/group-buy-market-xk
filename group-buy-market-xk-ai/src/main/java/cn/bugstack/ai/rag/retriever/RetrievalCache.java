package cn.bugstack.ai.rag.retriever;

import cn.bugstack.ai.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检索结果 TTL 缓存（Phase2）：同意图同问题短时间内复用检索结果，降低 LLM/检索成本
 */
@Component
public class RetrievalCache {

    private final AiProperties properties;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public RetrievalCache(AiProperties properties) {
        this.properties = properties;
    }

    public List<ScoredChunk> get(String category, String question) {
        String key = key(category, question);
        Entry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.timestamp > properties.getRetrieval().getCacheTtlSeconds() * 1000L) {
            cache.remove(key);
            return null;
        }
        return entry.chunks;
    }

    public void put(String category, String question, List<ScoredChunk> chunks) {
        cache.put(key(category, question), new Entry(chunks, System.currentTimeMillis()));
    }

    private String key(String category, String question) {
        return (category == null ? "*" : category) + "|" + (question == null ? "" : question.trim());
    }

    private static class Entry {
        final List<ScoredChunk> chunks;
        final long timestamp;

        Entry(List<ScoredChunk> chunks, long timestamp) {
            this.chunks = chunks;
            this.timestamp = timestamp;
        }
    }

}