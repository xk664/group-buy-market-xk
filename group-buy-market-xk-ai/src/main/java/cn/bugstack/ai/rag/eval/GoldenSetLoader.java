package cn.bugstack.ai.rag.eval;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Golden 测试集加载：解析 docs/ai-eval/golden-qa.md 的 Markdown 表格
 */
@Slf4j
public class GoldenSetLoader {

    public List<GoldenCase> load(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 Golden 测试集失败: " + path, e);
        }
        List<GoldenCase> cases = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.matches("\\|\\s*G\\d+\\s*\\|.*")) {
                continue;
            }
            String[] cells = trimmed.split("\\|");
            if (cells.length < 6) {
                continue;
            }
            cases.add(GoldenCase.builder()
                    .id(cells[1].trim())
                    .category(cells[2].trim())
                    .question(cells[3].trim())
                    .expectedDoc(cells[4].trim())
                    .expectedSection(cells[5].trim())
                    .referenceAnswer(cells.length > 6 ? cells[6].trim() : "")
                    .build());
        }
        return cases;
    }

}