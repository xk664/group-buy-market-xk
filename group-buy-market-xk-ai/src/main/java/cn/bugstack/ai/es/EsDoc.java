package cn.bugstack.ai.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ES 索引文档（对应一个知识 chunk）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsDoc {

    private long chunkId;
    private long docId;
    private String docTitle;
    private String chunkText;
    private String sectionPath;
    private String category;
    private String sourcePath;
    private String metadataJson;

}