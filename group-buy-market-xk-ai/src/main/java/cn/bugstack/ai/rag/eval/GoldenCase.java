package cn.bugstack.ai.rag.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Golden 评估用例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenCase {

    private String id;
    private String category;
    private String question;
    private String expectedDoc;
    private String expectedSection;
    private String referenceAnswer;

}