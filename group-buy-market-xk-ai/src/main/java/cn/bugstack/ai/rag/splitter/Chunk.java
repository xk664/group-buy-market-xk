package cn.bugstack.ai.rag.splitter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识切块
 * parentIndex：所属父块在切分结果列表中的下标；-1 表示自身是父块/普通块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    private String text;
    private String sectionPath;
    private int index;
    /** @Builder.Default 保证普通块/父块默认 parentIndex=-1（Lombok Builder 不读字段初始值） */
    @Builder.Default
    private int parentIndex = -1;

}