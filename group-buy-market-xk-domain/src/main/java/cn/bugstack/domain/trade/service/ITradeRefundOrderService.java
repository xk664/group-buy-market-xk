package cn.bugstack.domain.trade.service;

import cn.bugstack.domain.trade.model.entity.TradeRefundBehaviorEntity;
import cn.bugstack.domain.trade.model.entity.TradeRefundCommandEntity;

public interface ITradeRefundOrderService {
    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity);
}
