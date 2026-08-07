package cn.bugstack.ai.rag;

import cn.bugstack.ai.config.AiProperties;
import cn.bugstack.ai.rag.parser.ParsedDocument;
import cn.bugstack.ai.rag.parser.Section;
import cn.bugstack.ai.rag.splitter.Chunk;
import cn.bugstack.ai.rag.splitter.RecursiveCharacterTextSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 结构化切分编排：按文档类型选择切分策略
 * - FAQ：一问一答一个 chunk，绝不拆分
 * - PARAM_TABLE：整表一个 chunk
 * - 政策/规则/商品详情：标题树叶子切分；
 *   长 section（> parentThreshold）启用【父子 chunk】：
 *   父块 = 整节（上下文完整），子块 = 递归小块（检索精准，parentIndex 指向父块）
 */
public class ChunkingService {

    private static final int MAX_SECTION_CHARS = 800;
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private static final List<String> PARENT_CHILD_TYPES = Arrays.asList("POLICY", "RULE", "PRODUCT_DETAIL");

    private final AiProperties.Chunking config;

    public ChunkingService() {
        this.config = new AiProperties.Chunking();
    }

    public ChunkingService(AiProperties.Chunking config) {
        this.config = config == null ? new AiProperties.Chunking() : config;
    }

    public List<Chunk> split(ParsedDocument doc) {
        String docType = doc.getDocType() == null ? "" : doc.getDocType().toUpperCase(Locale.ROOT);
        if ("FAQ".equals(docType)) {
            return splitFaq(doc);
        }
        if ("PARAM_TABLE".equals(docType)) {
            return splitParamTable(doc);
        }
        return splitBySections(doc, docType);
    }

    private List<Chunk> splitFaq(ParsedDocument doc) {
        List<Chunk> chunks = new ArrayList<Chunk>();
        List<String> lines = Arrays.asList(doc.getRawText().split("\\r?\\n"));
        StringBuilder current = new StringBuilder();
        boolean started = false;
        int index = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## Q") || trimmed.startsWith("## Q：")) {
                if (started && current.length() > 0) {
                    chunks.add(Chunk.builder().text(current.toString().trim()).sectionPath(doc.getTitle()).index(index++).build());
                }
                started = true;
                current = new StringBuilder();
            }
            if (started) {
                current.append(line).append('\n');
            }
        }
        if (started && current.length() > 0) {
            chunks.add(Chunk.builder().text(current.toString().trim()).sectionPath(doc.getTitle()).index(index).build());
        }
        return chunks;
    }

    private List<Chunk> splitParamTable(ParsedDocument doc) {
        String text = doc.getRawText().trim();
        if (text.isEmpty()) {
            return new ArrayList<Chunk>();
        }
        List<Chunk> chunks = new ArrayList<Chunk>();
        chunks.add(Chunk.builder().text(text).sectionPath(doc.getTitle()).index(0).build());
        return chunks;
    }

    private List<Chunk> splitBySections(ParsedDocument doc, String docType) {
        List<Chunk> chunks = new ArrayList<Chunk>();
        int index = 0;
        for (Section section : doc.getSections()) {
            if (!section.hasContent()) {
                continue;
            }
            String content = section.content();
            String prefix = "# " + section.getTitle() + "\n";
            boolean parentChild = Boolean.TRUE.equals(config.getParentChildEnabled())
                    && PARENT_CHILD_TYPES.contains(docType)
                    && content.length() > config.getParentThreshold();

            if (parentChild) {
                // 父块：整节全文（上下文完整）
                int parentIndex = index;
                chunks.add(Chunk.builder()
                        .text(prefix + content)
                        .sectionPath(section.getPath())
                        .index(index++)
                        .parentIndex(-1)
                        .build());
                // 子块：小块递归切分，parentIndex 指向父块
                RecursiveCharacterTextSplitter childSplitter =
                        new RecursiveCharacterTextSplitter(config.getChildSize(), config.getChildOverlap());
                for (String part : childSplitter.splitText(content)) {
                    chunks.add(Chunk.builder()
                            .text(prefix + part)
                            .sectionPath(section.getPath())
                            .index(index++)
                            .parentIndex(parentIndex)
                            .build());
                }
            } else if (content.length() <= MAX_SECTION_CHARS) {
                chunks.add(Chunk.builder()
                        .text(prefix + content)
                        .sectionPath(section.getPath())
                        .index(index++)
                        .build());
            } else {
                RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(CHUNK_SIZE, CHUNK_OVERLAP);
                for (String part : splitter.splitText(content)) {
                    chunks.add(Chunk.builder()
                            .text(prefix + part)
                            .sectionPath(section.getPath())
                            .index(index++)
                            .build());
                }
            }
        }
        if (chunks.isEmpty()) {
            RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(CHUNK_SIZE, CHUNK_OVERLAP);
            for (String part : splitter.splitText(doc.getRawText())) {
                chunks.add(Chunk.builder().text(part).sectionPath(doc.getTitle()).index(index++).build());
            }
        }
        return chunks;
    }

}