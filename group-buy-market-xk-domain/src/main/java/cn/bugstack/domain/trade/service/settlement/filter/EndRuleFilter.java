package cn.bugstack.domain.trade.service.settlement.filter;

import cn.bugstack.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.bugstack.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {
    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-结束节点{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        return TradeSettlementRuleFilterBackEntity.builder()
                .completeCount(dynamicContext.getGroupBuyTeamEntity().getCompleteCount())
                .lockCount(dynamicContext.getGroupBuyTeamEntity().getLockCount())
                .targetCount(dynamicContext.getGroupBuyTeamEntity().getTargetCount())
                .status(dynamicContext.getGroupBuyTeamEntity().getStatus())
                .validStartTime(dynamicContext.getGroupBuyTeamEntity().getValidStartTime())
                .validEndTime(dynamicContext.getGroupBuyTeamEntity().getValidEndTime())
                .teamId(dynamicContext.getGroupBuyTeamEntity().getTeamId())
                .activityId(dynamicContext.getGroupBuyTeamEntity().getActivityId())
                .notifyUrl(dynamicContext.getGroupBuyTeamEntity().getNotifyUrl())
                .build();
    }
}
