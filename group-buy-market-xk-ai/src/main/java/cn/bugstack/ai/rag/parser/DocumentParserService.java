package cn.bugstack.ai.rag.parser;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 知识文档解析器：
 * 1. 解析 front matter（--- key: value ---）
 * 2. 解析标题树（#~####），生成带完整路径的 Section
 */
@Slf4j
public class DocumentParserService {

    private static final String FM_START = "---";
    private static final int MAX_HEADER_LEVEL = 4;

    public ParsedDocument parse(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取知识文档失败: " + path, e);
        }

        FrontMatter fm = new FrontMatter();
        int bodyStart = parseFrontMatter(lines, fm);

        List<Section> sections = new ArrayList<>();
        StringBuilder raw = new StringBuilder();
        List<int[]> stack = new ArrayList<>(); // [level, sectionIndex]
        Section current = null;

        for (int i = bodyStart; i < lines.size(); i++) {
            String line = lines.get(i);
            String header = headerOf(line);
            if (header != null) {
                int level = headerLevel(line);
                Section section = new Section(level, header, "");
                while (!stack.isEmpty() && stack.get(stack.size() - 1)[0] >= level) {
                    stack.remove(stack.size() - 1);
                }
                StringBuilder sectionPath = new StringBuilder();
                for (int[] frame : stack) {
                    Section ancestor = sections.get(frame[1]);
                    if (sectionPath.length() > 0) {
                        sectionPath.append(" > ");
                    }
                    sectionPath.append(ancestor.getTitle());
                }
                if (sectionPath.length() > 0) {
                    sectionPath.append(" > ");
                }
                sectionPath.append(header);
                section = new Section(level, header, sectionPath.toString());
                sections.add(section);
                stack.add(new int[]{level, sections.size() - 1});
                current = section;
                raw.append(line).append('\n');
            } else if (current != null) {
                current.appendLine(line);
                raw.append(line).append('\n');
            } else {
                // 首个标题之前的内容（如导语），保留到 raw，不归属任何 section
                raw.append(line).append('\n');
            }
        }

        String title = fm.title();
        if (title == null || title.trim().isEmpty()) {
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    title = line.substring(2).trim();
                    break;
                }
            }
        }
        if (title == null || title.trim().isEmpty()) {
            title = path.getFileName().toString().replace(".md", "");
        }
        if (sections.isEmpty() && raw.length() > 0) {
            sections.add(new Section(1, title, title));
        }

        return ParsedDocument.builder()
                .sourcePath(path.toString())
                .fileName(path.getFileName().toString())
                .title(title)
                .category(fm.category())
                .docType(fm.docType())
                .docVersion(fm.docVersion())
                .effectiveDate(fm.effectiveDate())
                .productId(fm.productId())
                .sourceUrl(fm.sourceUrl())
                .sections(sections)
                .rawText(raw.toString())
                .build();
    }

    private int parseFrontMatter(List<String> lines, FrontMatter fm) {
        if (lines.isEmpty() || !FM_START.equals(lines.get(0).trim())) {
            return 0;
        }
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (FM_START.equals(line.trim())) {
                return i + 1;
            }
            int idx = line.indexOf(':');
            if (idx > 0) {
                fm.put(line.substring(0, idx), line.substring(idx + 1));
            }
        }
        return 1;
    }

    private String headerOf(String line) {
        if (line.startsWith("#") && !line.startsWith("##") || line.startsWith("## ")) {
            // 统一走 headerLevel 判断
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        int level = headerLevel(line);
        if (level < 1 || level > MAX_HEADER_LEVEL || line.length() <= level) {
            return null;
        }
        String title = line.substring(level).trim();
        return title.isEmpty() ? null : title;
    }

    private int headerLevel(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        return level;
    }

}