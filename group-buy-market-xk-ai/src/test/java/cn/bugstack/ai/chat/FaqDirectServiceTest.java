package cn.bugstack.ai.chat;

import cn.bugstack.ai.rag.retriever.ScoredChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FaqDirectServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FaqDirectService service = new FaqDirectService(objectMapper);

    @Test
    public void testExtractFaq() {
        ScoredChunk chunk = ScoredChunk.builder()
                .metadataJson("{\"doc_type\":\"FAQ\"}")
                .chunkText("## Q: 退款一般多久到账？\nA: 微信/支付宝 1-3 个工作日到账。")
                .build();
        Optional<String> answer = service.extract(chunk);
        assertTrue(answer.isPresent());
        assertEquals("微信/支付宝 1-3 个工作日到账。", answer.get());
    }

    @Test
    public void testNonFaqReturnsEmpty() {
        ScoredChunk chunk = ScoredChunk.builder()
                .metadataJson("{\"doc_type\":\"POLICY\"}")
                .chunkText("A: 某条款")
                .build();
        assertFalse(service.extract(chunk).isPresent());
    }

    @Test
    public void testNullMetadataReturnsEmpty() {
        ScoredChunk chunk = ScoredChunk.builder().chunkText("A: x").build();
        assertFalse(service.extract(chunk).isPresent());
    }

}