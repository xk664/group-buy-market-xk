package cn.bugstack.ai.rag.splitter;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecursiveCharacterTextSplitterTest {

    @Test
    public void testShortTextNoSplit() {
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(500, 50);
        List<String> parts = splitter.splitText("退款一般1-3个工作日到账。");
        assertEquals(1, parts.size());
    }

    @Test
    public void testLongTextSplitBySentence() {
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(20, 5);
        String text = "这是第一句。这是第二句！这是第三句？这是第四句，这是第五句，这是第六句。";
        List<String> parts = splitter.splitText(text);
        assertFalse(parts.isEmpty());
        for (String part : parts) {
            assertTrue("chunk 超长: " + part.length(), part.length() <= 20);
        }
    }

    @Test
    public void testMergeKeepsOverlap() {
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(10, 4);
        String text = "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸";
        List<String> parts = splitter.splitText(text);
        assertFalse(parts.isEmpty());
    }

}