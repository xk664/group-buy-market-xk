package cn.bugstack.ai.rag;

import cn.bugstack.ai.rag.parser.ParsedDocument;
import cn.bugstack.ai.rag.parser.Section;
import cn.bugstack.ai.rag.splitter.Chunk;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChunkingServiceParentChildTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    public void testLongPolicySectionCreatesParentAndChildren() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longContent.append("这里是第").append(i).append("条退款规则说明，内容用于测试父子切分。");
        }
        Section section = new Section(2, "二、退款条件", "退款规则 > 二、退款条件");
        section.appendLine(longContent.toString());

        ParsedDocument doc = ParsedDocument.builder()
                .title("退款规则")
                .category("REFUND")
                .docType("POLICY")
                .sections(Collections.singletonList(section))
                .rawText(longContent.toString())
                .build();

        List<Chunk> chunks = chunkingService.split(doc);
        assertTrue("应产生父块+多个子块", chunks.size() >= 2);

        // 第一个是父块（parentIndex=-1），文本为整节全文
        Chunk parent = chunks.get(0);
        assertEquals(-1, parent.getParentIndex());
        assertTrue(parent.getText().length() > 400);

        // 其余是子块：parentIndex 指向父块下标 0，且为小块
        for (int i = 1; i < chunks.size(); i++) {
            assertEquals("子块 parentIndex 应指向父块", 0, chunks.get(i).getParentIndex());
            assertTrue("子块应是小块: " + chunks.get(i).getText().length(),
                    chunks.get(i).getText().length() <= 210);
        }
    }

    @Test
    public void testShortPolicySectionNoParentChild() {
        Section section = new Section(1, "一、适用范围", "退款规则 > 一、适用范围");
        section.appendLine("本规则适用于平台所有订单。");
        ParsedDocument doc = ParsedDocument.builder()
                .title("退款规则")
                .docType("POLICY")
                .sections(Collections.singletonList(section))
                .rawText("本规则适用于平台所有订单。")
                .build();
        List<Chunk> chunks = chunkingService.split(doc);
        assertEquals(1, chunks.size());
        assertEquals(-1, chunks.get(0).getParentIndex());
    }

    @Test
    public void testFaqNotParentChild() {
        String faqText = "## Q: 退款一般多久到账？\nA: 微信/支付宝 1-3 个工作日到账。\n"
                + "## Q: 拼团失败会退款吗？\nA: 会，自动原路退回。";
        ParsedDocument doc = ParsedDocument.builder()
                .title("高频问答")
                .docType("FAQ")
                .sections(new ArrayList<Section>())
                .rawText(faqText)
                .build();
        List<Chunk> chunks = chunkingService.split(doc);
        assertEquals(2, chunks.size());
        for (Chunk chunk : chunks) {
            assertEquals(-1, chunk.getParentIndex());
        }
    }

}