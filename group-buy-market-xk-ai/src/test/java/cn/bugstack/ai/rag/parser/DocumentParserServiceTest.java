package cn.bugstack.ai.rag.parser;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DocumentParserServiceTest {

    @Test
    public void testParseFrontMatterAndSections() throws Exception {
        String md = "---\n"
                + "category: REFUND\n"
                + "doc_type: POLICY\n"
                + "doc_version: \"2026-07\"\n"
                + "effective_date: \"2026-07-01\"\n"
                + "title: 退款规则\n"
                + "---\n"
                + "# 退款规则\n"
                + "## 二、退款条件\n"
                + "### 2.1 未发货订单\n"
                + "订单支付成功后、商家发货前，可申请全额退款。\n"
                + "## 三、退款流程\n"
                + "1. 提交申请\n";
        Path tmp = Files.createTempFile("doc-test", ".md");
        Files.write(tmp, md.getBytes(StandardCharsets.UTF_8));

        DocumentParserService parser = new DocumentParserService();
        ParsedDocument doc = parser.parse(tmp);

        assertEquals("REFUND", doc.getCategory());
        assertEquals("2026-07", doc.getDocVersion());
        assertEquals("退款规则", doc.getTitle());
        assertEquals(4, doc.getSections().size());

        Section s = doc.getSections().get(1);
        assertEquals("二、退款条件", s.getTitle());
        assertEquals(2, s.getLevel());
        assertTrue(s.getPath().contains("退款规则"));
        assertTrue(s.getPath().contains("二、退款条件"));
        assertNotNull(doc.getRawText());
    }

}