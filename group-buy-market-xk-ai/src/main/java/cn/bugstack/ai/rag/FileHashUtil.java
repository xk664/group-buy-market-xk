package cn.bugstack.ai.rag;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * 文件/文本指纹工具（SHA-256，流式计算，内存占用固定）
 */
public final class FileHashUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private FileHashUtil() {
    }

    /** 计算文件字节流指纹（边读边算，不整载入内存） */
    public static String sha256(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            try (InputStream in = Files.newInputStream(path)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            return hex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("计算文件指纹失败: " + path, e);
        }
    }

    /** 计算文本指纹 */
    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
            return hex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("计算文本指纹失败", e);
        }
    }

    private static String hex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            chars[i * 2] = HEX[v >>> 4];
            chars[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(chars);
    }

}