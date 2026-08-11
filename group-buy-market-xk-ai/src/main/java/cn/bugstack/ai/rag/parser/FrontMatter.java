package cn.bugstack.ai.rag.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档 front matter 元数据（--- 块中 key: value）
 */
public class FrontMatter {

    private final Map<String, String> values = new HashMap<>();

    public void put(String key, String value) {
        if (key != null) {
            values.put(key.trim(), value == null ? null : unquote(value.trim()));
        }
    }

    public String get(String key) {
        return values.get(key);
    }

    public String category() {
        return values.get("category");
    }

    public String docType() {
        return values.get("doc_type");
    }

    public String docVersion() {
        return values.get("doc_version");
    }

    public String effectiveDate() {
        return values.get("effective_date");
    }

    public String title() {
        return values.get("title");
    }

    public String productId() {
        return values.get("product_id");
    }

    public String sourceUrl() {
        return values.get("source_url");
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private String unquote(String v) {
        if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

}