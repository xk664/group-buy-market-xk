package cn.bugstack.ai.rag.retriever;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索结果块（含融合分数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredChunk {

    private Long chunkId;
    private Long docId;
    private String docTitle;
    private String chunkText;
    private String sectionPath;
    private String metadataJson;
    private Long parentId;
    private Double score;

}