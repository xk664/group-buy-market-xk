package cn.bugstack.domain.trade.service.refund;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.RefundTypeEnumVO;
import cn.bugstack.domain.trade.model.valobj.TeamRefundSuccess;
import cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.bugstack.domain.trade.service.ITradeRefundOrderService;
import cn.bugstack.domain.trade.service.refund.business.IRefundOrderStrategy;
import cn.bugstack.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.bugstack.types.enums.GroupBuyOrderEnumVO;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
@Slf4j
public class TradeRefundOrderService implements ITradeRefundOrderService {

    private final  ITradeRepository repository;

    private final Map<String, IRefundOrderStrategy> refundOrderStrategyMap;
    @Resource
    private BusinessLinkedList<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> tradeRefundOrderChain;

    public  TradeRefundOrderService(ITradeRepository repository, Map<String, IRefundOrderStrategy> refundOrderStrategyMap) {
        this.repository = repository;
        this.refundOrderStrategyMap = refundOrderStrategyMap;
    }
    @Override
    public TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception {
        log.info("逆向流程,交易退单，用户Id{}，外部单号{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        return tradeRefundOrderChain.apply(tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext.builder().build());
    }

    @Override
    public void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        log.info("逆向流程，恢复锁单量 userId:{} activityId:{} teamId:{}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        //获取退单类型
        String type = teamRefundSuccess.getType();

        RefundTypeEnumVO refundTypeEnumVOByCode = RefundTypeEnumVO.getRefundTypeEnumVOByCode(type);
        IRefundOrderStrategy iRefundOrderStrategy = refundOrderStrategyMap.get(refundTypeEnumVOByCode.getStrategy());
        iRefundOrderStrategy.reverseStock(teamRefundSuccess);
    }
}
