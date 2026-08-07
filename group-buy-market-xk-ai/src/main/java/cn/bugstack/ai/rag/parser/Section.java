package cn.bugstack.ai.rag.parser;

import lombok.Getter;

/**
 * Markdown 章节（含层级与完整路径）
 */
@Getter
public class Section {

    private final int level;
    private final String title;
    /** 完整路径，如：退款规则 > 二、退款条件 > 2.1 未发货订单 */
    private final String path;
    private final StringBuilder content = new StringBuilder();

    public Section(int level, String title, String path) {
        this.level = level;
        this.title = title;
        this.path = path;
    }

    public void appendLine(String line) {
        content.append(line).append('\n');
    }

    public String content() {
        return content.toString().trim();
    }

    public boolean hasContent() {
        return content.length() > 0;
    }

}