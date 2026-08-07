package cn.bugstack.ai.chat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuleQueryAnalyzerTest {

    private final RuleQueryAnalyzer analyzer = new RuleQueryAnalyzer();

    @Test
    public void testRefund() {
        QueryPlan plan = analyzer.analyze("u1", "退款一般多久到账？");
        assertEquals(Intent.REFUND, plan.getIntent());
        assertEquals("REFUND", plan.getCategoryFilter());
    }

    @Test
    public void testGroupon() {
        QueryPlan plan = analyzer.analyze("u1", "拼团失败会退款吗？");
        assertEquals(Intent.GROUPON, plan.getIntent());
    }

    @Test
    public void testProduct() {
        QueryPlan plan = analyzer.analyze("u1", "耳机支持无线充电吗？");
        assertEquals(Intent.PRODUCT, plan.getIntent());
    }

    @Test
    public void testStatusNeedTool() {
        QueryPlan plan = analyzer.analyze("u1", "我的订单为什么还没退款？");
        assertTrue(plan.isNeedTool());
    }

    @Test
    public void testComplaintOther() {
        QueryPlan plan = analyzer.analyze("u1", "我要投诉你们！");
        assertEquals(Intent.OTHER, plan.getIntent());
    }

}