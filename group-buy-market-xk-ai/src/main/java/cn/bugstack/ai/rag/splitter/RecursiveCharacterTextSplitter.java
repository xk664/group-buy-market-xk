package cn.bugstack.ai.rag.splitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 中文场景递归字符切分器
 * 分隔符优先级：\n\n → \n → 。！？； → ， → 空格 → 字符
 */
public class RecursiveCharacterTextSplitter {

    private static final List<String> DEFAULT_SEPARATORS = Collections.unmodifiableList(Arrays.asList(
            "\n\n", "\n", "。", "！", "？", "；", "，", " ", ""));

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this(chunkSize, chunkOverlap, DEFAULT_SEPARATORS);
    }

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap, List<String> separators) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators;
    }

    public List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> raw = splitInternal(text.trim(), 0);
        return merge(raw);
    }

    private List<String> splitInternal(String text, int sepIndex) {
        if (text.length() <= chunkSize) {
            return Collections.singletonList(text);
        }
        if (sepIndex >= separators.size()) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < text.length(); i += chunkSize) {
                parts.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
            }
            return parts;
        }
        String separator = separators.get(sepIndex);
        List<String> splits = splitBySeparator(text, separator);
        List<String> results = new ArrayList<>();
        boolean hasMore = sepIndex + 1 < separators.size();
        for (String split : splits) {
            if (split.length() > chunkSize && hasMore) {
                results.addAll(splitInternal(split, sepIndex + 1));
            } else {
                results.add(split);
            }
        }
        return results;
    }

    private List<String> splitBySeparator(String text, String separator) {
        if (separator.isEmpty()) {
            List<String> chars = new ArrayList<>();
            for (int i = 0; i < text.length(); i++) {
                chars.add(String.valueOf(text.charAt(i)));
            }
            return chars;
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        int idx;
        while ((idx = text.indexOf(separator, start)) >= 0) {
            parts.add(text.substring(start, idx));
            start = idx + separator.length();
        }
        if (start < text.length()) {
            parts.add(text.substring(start));
        } else if (text.endsWith(separator)) {
            parts.add("");
        }
        if (parts.isEmpty()) {
            parts.add(text);
        }
        return parts;
    }

    private List<String> merge(List<String> chunks) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }
            if (current.length() == 0 || current.length() + chunk.length() <= chunkSize) {
                if (current.length() > 0) {
                    current.append('\n');
                }
                current.append(chunk);
            } else {
                merged.add(current.toString());
                String overlap = current.length() > chunkOverlap
                        ? current.substring(current.length() - chunkOverlap)
                        : current.toString();
                current = new StringBuilder(overlap);
                if (current.length() > 0) {
                    current.append('\n');
                }
                current.append(chunk);
            }
        }
        if (current.length() > 0) {
            merged.add(current.toString());
        }
        return enforceMaxSize(merged);
    }

    /** 兜底：保证任意 chunk 不超过 chunkSize（极端长词/无分隔符场景） */
    private List<String> enforceMaxSize(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() <= chunkSize) {
                result.add(chunk);
                continue;
            }
            for (int i = 0; i < chunk.length(); i += chunkSize) {
                result.add(chunk.substring(i, Math.min(chunk.length(), i + chunkSize)));
            }
        }
        return result;
    }

}