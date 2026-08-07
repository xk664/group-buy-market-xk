package cn.bugstack.ai.rag.conflict;

import cn.bugstack.ai.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConflictJudgeTest {

    @Test
    public void testJudgeConflict() {
        LlmClient fake = new FakeLlmClient("{\"related\":true,\"conflict\":true,\"topic\":\"退款到账时效\",\"reason\":\"一个说1-3天，一个说3-5天\"}");
        ConflictJudge judge = new ConflictJudge(fake, new ObjectMapper());
        Optional<ConflictJudge.Verdict> verdict = judge.judge("退款1-3天到账", "退款3-5天到账");
        assertTrue(verdict.isPresent());
        assertTrue(verdict.get().isConflict());
        assertEquals("退款到账时效", verdict.get().getTopic());
    }

    @Test
    public void testJudgeNoConflict() {
        LlmClient fake = new FakeLlmClient("{\"related\":true,\"conflict\":false,\"topic\":\"\",\"reason\":\"\"}");
        ConflictJudge judge = new ConflictJudge(fake, new ObjectMapper());
        assertFalse(judge.judge("退款1-3天到账", "退款1-3天到账").isPresent());
    }

    @Test
    public void testMockReturnsEmpty() {
        FakeLlmClient fake = new FakeLlmClient("{}");
        fake.mock = true;
        ConflictJudge judge = new ConflictJudge(fake, new ObjectMapper());
        assertFalse(judge.judge("A", "B").isPresent());
    }

    private static class FakeLlmClient implements LlmClient {
        private final String response;
        private boolean mock = false;

        FakeLlmClient(String response) {
            this.response = response;
        }

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            return response;
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return new ArrayList<float[]>();
        }

        @Override
        public boolean isMock() {
            return mock;
        }
    }

}