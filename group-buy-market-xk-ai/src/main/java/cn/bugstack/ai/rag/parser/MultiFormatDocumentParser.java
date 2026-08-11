package cn.bugstack.ai.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 多格式文档解析器：
 * - .md/.markdown：走结构化 Markdown 解析（front matter + 标题树）
 * - .txt：纯文本，按整篇入库（切分阶段再递归切分）
 * - .pdf/.doc/.docx：Apache Tika 抽取文本
 * - .xls/.xlsx：Tika 抽取后自动转 Markdown 表格（表头+分隔行+数据行）
 * 非 Markdown 文档的 category/doc_type 由入库服务按目录/文件名归一化
 */
@Slf4j
@Component
public class MultiFormatDocumentParser {

    private static final List<String> MARKDOWN_EXTS = Arrays.asList("md", "markdown");
    private static final List<String> TEXT_EXTS = Arrays.asList("txt");
    private static final List<String> EXCEL_EXTS = Arrays.asList("xls", "xlsx");
    private static final List<String> TIKA_EXTS = Arrays.asList("pdf", "doc", "docx");

    private final DocumentParserService markdownParser = new DocumentParserService();
    private final Tika tika = new Tika();

    public ParsedDocument parse(Path path) {
        String ext = extension(path);
        if (MARKDOWN_EXTS.contains(ext)) {
            return markdownParser.parse(path);
        }
        if (TEXT_EXTS.contains(ext)) {
            return parsePlainText(path);
        }
        if (EXCEL_EXTS.contains(ext)) {
            return buildDocument(path, toMarkdownTable(parseWithTika(path)));
        }
        if (TIKA_EXTS.contains(ext)) {
            return buildDocument(path, parseWithTika(path));
        }
        throw new IllegalArgumentException("不支持的文档格式: " + path + " (ext=" + ext + ")");
    }

    public boolean isSupported(Path path) {
        String ext = extension(path);
        return MARKDOWN_EXTS.contains(ext) || TEXT_EXTS.contains(ext)
                || EXCEL_EXTS.contains(ext) || TIKA_EXTS.contains(ext);
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private ParsedDocument parsePlainText(Path path) {
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return buildDocument(path, text);
        } catch (Exception e) {
            throw new IllegalStateException("读取文本文件失败: " + path, e);
        }
    }

    private String parseWithTika(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return tika.parseToString(in);
        } catch (Exception e) {
            throw new IllegalStateException("Tika 解析文档失败: " + path, e);
        }
    }

    private ParsedDocument buildDocument(Path path, String text) {
        String fileName = path.getFileName().toString();
        int idx = fileName.lastIndexOf('.');
        String title = idx < 0 ? fileName : fileName.substring(0, idx);
        return ParsedDocument.builder()
                .sourcePath(path.toString())
                .fileName(fileName)
                .title(title)
                .sections(Collections.emptyList())
                .rawText(text == null ? "" : text.trim())
                .build();
    }

    /**
     * Tika 的 Excel 输出：行=换行，列=制表符。转成 Markdown 表格便于检索保留结构。
     */
    private String toMarkdownTable(String tikaText) {
        if (tikaText == null || tikaText.trim().isEmpty()) {
            return tikaText;
        }
        String[] lines = tikaText.trim().split("\\r?\\n");
        List<String[]> rows = new ArrayList<String[]>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            rows.add(line.split("\\t", -1));
        }
        if (rows.size() < 2) {
            return tikaText.trim();
        }
        StringBuilder sb = new StringBuilder();
        String[] header = rows.get(0);
        sb.append("| ").append(joinCells(header)).append(" |\n");
        StringBuilder sep = new StringBuilder("|");
        for (int i = 0; i < header.length; i++) {
            sep.append("---|");
        }
        sb.append(sep).append('\n');
        for (int i = 1; i < rows.size(); i++) {
            sb.append("| ").append(joinCells(rows.get(i))).append(" |\n");
        }
        return sb.toString();
    }

    private String joinCells(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(cells[i].replace("|", "\\|").trim());
        }
        return sb.toString();
    }

}