package cn.bugstack.domain.activity.service.discount;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface IDiscountCalculateService {
     BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);
}
