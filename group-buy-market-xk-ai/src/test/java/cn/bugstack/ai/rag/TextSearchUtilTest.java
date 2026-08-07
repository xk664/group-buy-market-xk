package cn.bugstack.ai.rag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextSearchUtilTest {

    @Test
    public void testToSearchText() {
        String searchText = TextSearchUtil.toSearchText("退款到账");
        assertEquals("退 款 到 账 ", searchText);
    }

    @Test
    public void testToTsQuery() {
        String tsQuery = TextSearchUtil.toTsQuery("退款到账");
        assertEquals("退 & 款 & 到 & 账", tsQuery);
    }

    @Test
    public void testMixedText() {
        String searchText = TextSearchUtil.toSearchText("SKU13811216退款");
        assertTrue(searchText.contains("SKU13811216"));
        assertTrue(searchText.contains("退 "));
    }

}