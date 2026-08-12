package cn.bugstack.ai.chat;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则版查询分析（Phase1）：
 * - 状态类（我的订单/进度/差几人/为什么还没）→ needTool=true → 人工处理中...
 * - 投诉/闲聊 → OTHER → 人工处理中...
 * - 其余按关键词命中数判定 REFUND / PRODUCT / GROUPON
 */
@Component
public class RuleQueryAnalyzer implements QueryAnalyzer {

    private static final List<String> TOOL_KEYWORDS = Arrays.asList(
            "我的订单", "我的退款", "我的拼团", "我的团", "进度", "差几", "还剩", "为什么还没", "到哪一步", "状态", "到哪里了");
    private static final List<String> OTHER_KEYWORDS = Arrays.asList(
            "投诉", "人工", "客服电话", "转人工", "你好", "您好", "谢谢", "再见", "在吗");
    private static final List<String> REFUND_KEYWORDS = Arrays.asList(
            "退款", "退货", "退换", "退钱", "退运费", "售后", "无理由", "极速退款", "发票", "换货", "退回");
    private static final List<String> GROUPON_KEYWORDS = Arrays.asList(
            "拼团", "成团", "开团", "参团", "团购", "拼单", "拼价", "限购", "拼团失败", "几人", "团");
    private static final List<String> PRODUCT_KEYWORDS = Arrays.asList(
            "商品", "参数", "规格", "续航", "材质", "蓝牙", "键盘", "保温杯", "耳机", "充电", "快充",
            "防水", "颜色", "尺码", "型号", "sku", "多少钱", "价格", "库存", "支持", "质保");

    @Override
    public QueryPlan analyze(String userId, String question) {
        String q = question == null ? "" : question.trim();
        String lower = q.toLowerCase();

        for (String kw : OTHER_KEYWORDS) {
            if (lower.contains(kw)) {
                return plan(Intent.OTHER, null, q, false, null);
            }
        }
        for (String kw : TOOL_KEYWORDS) {
            if (lower.contains(kw)) {
                return plan(Intent.OTHER, null, q, true, null);
            }
        }

        int refund = count(REFUND_KEYWORDS, lower);
        int groupon = count(GROUPON_KEYWORDS, lower);
        int product = count(PRODUCT_KEYWORDS, lower);
        Intent intent;
        if (refund >= groupon && refund >= product && refund > 0) {
            intent = Intent.REFUND;
        } else if (groupon >= product && groupon > 0) {
            intent = Intent.GROUPON;
        } else if (product > 0) {
            intent = Intent.PRODUCT;
        } else {
            intent = Intent.OTHER;
        }

        Map<String, String> entities = new HashMap<>();
        return plan(intent, intent == Intent.OTHER ? null : intent.name(), q, false, entities);
    }

    private QueryPlan plan(Intent intent, String category, String q, boolean needTool, Map<String, String> entities) {
        return QueryPlan.builder()
                .intent(intent)
                .categoryFilter(category)
                .rewrittenQuery(q)
                .needTool(needTool)
                .entities(entities)
                .build();
    }

    private int count(List<String> keywords, String lower) {
        int n = 0;
        for (String kw : keywords) {
            if (lower.contains(kw)) {
                n++;
            }
        }
        return n;
    }

}
