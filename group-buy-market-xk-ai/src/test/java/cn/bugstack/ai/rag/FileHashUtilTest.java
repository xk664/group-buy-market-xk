package cn.bugstack.ai.rag;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FileHashUtilTest {

    @Test
    public void testSameTextSameHash() {
        assertEquals(FileHashUtil.sha256("退款3天到账"), FileHashUtil.sha256("退款3天到账"));
    }

    @Test
    public void testDiffTextDiffHash() {
        assertNotEquals(FileHashUtil.sha256("退款3天到账"), FileHashUtil.sha256("退款5天到账"));
    }

    @Test
    public void testStreamingFileHash() throws Exception {
        Path tmp = Files.createTempFile("hash-test", ".txt");
        Files.write(tmp, "退款规则：1-3 个工作日到账。".getBytes(StandardCharsets.UTF_8));
        String h1 = FileHashUtil.sha256(tmp);
        assertEquals(64, h1.length());
        // 同文件重复计算一致
        assertEquals(h1, FileHashUtil.sha256(tmp));
        // 内容变化后指纹变化
        Files.write(tmp, "退款规则：3-5 个工作日到账。".getBytes(StandardCharsets.UTF_8));
        assertNotEquals(h1, FileHashUtil.sha256(tmp));
    }

}