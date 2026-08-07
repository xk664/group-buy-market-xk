package cn.bugstack.ai.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 答案引用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reference {

    private String title;
    private String sectionPath;

}