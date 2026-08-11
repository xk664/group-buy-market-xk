package cn.bugstack.ai.rag;

/**
 * 中文全文检索文本工具：
 * - toSearchText：CJK 字符间插入空格，使 PG simple 配置可按字符级分词
 * - toTsQuery：查询词转 tsquery（词之间用 & 连接）
 */
public final class TextSearchUtil {

    private TextSearchUtil() {
    }

    public static String toSearchText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                sb.append(c).append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toTsQuery(String query) {
        String searchText = toSearchText(query).trim();
        if (searchText.isEmpty()) {
            return "";
        }
        String[] words = searchText.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" & ");
            }
            sb.append(word);
        }
        return sb.toString();
    }

}