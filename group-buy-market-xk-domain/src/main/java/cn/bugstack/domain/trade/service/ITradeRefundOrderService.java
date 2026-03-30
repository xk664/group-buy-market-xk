package cn.bugstack.domain.trade.service;

import cn.bugstack.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.bugstack.domain.trade.model.entity.TradeRefundCommandEntity;
import cn.bugstack.domain.trade.model.valobj.TeamRefundSuccess;

public interface ITradeRefundOrderService {
    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

    void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception;
}
