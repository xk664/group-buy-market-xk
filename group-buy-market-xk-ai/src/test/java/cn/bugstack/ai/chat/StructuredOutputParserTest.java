package cn.bugstack.ai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StructuredOutputParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testParseJson() {
        String raw = "好的，回答如下：\n{\"answer\":\"退款一般1-3个工作日到账\",\"need_human\":false}";
        Optional<StructuredAnswer> result = StructuredOutputParser.parse(raw, objectMapper);
        assertTrue(result.isPresent());
        assertEquals("退款一般1-3个工作日到账", result.get().getAnswer());
        assertFalse(result.get().isNeedHuman());
    }

    @Test
    public void testParseNeedHuman() {
        String raw = "{\"answer\":\"需要人工\",\"need_human\":true}";
        Optional<StructuredAnswer> result = StructuredOutputParser.parse(raw, objectMapper);
        assertTrue(result.isPresent());
        assertTrue(result.get().isNeedHuman());
    }

    @Test
    public void testPlainTextFallback() {
        Optional<StructuredAnswer> result = StructuredOutputParser.parse("这不是 JSON", objectMapper);
        assertFalse(result.isPresent());
    }

}