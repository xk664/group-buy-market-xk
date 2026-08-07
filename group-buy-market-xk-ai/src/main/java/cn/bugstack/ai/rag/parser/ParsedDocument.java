package cn.bugstack.ai.rag.parser;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 解析后的知识文档
 */
@Getter
@Builder
public class ParsedDocument {

    private String sourcePath;
    private String fileName;
    private String title;
    private String category;
    private String docType;
    private String docVersion;
    private String effectiveDate;
    private String productId;
    private String sourceUrl;
    private List<Section> sections;
    /** 去除 front matter 后的原始文本（FAQ/参数表切分用） */
    private String rawText;

}