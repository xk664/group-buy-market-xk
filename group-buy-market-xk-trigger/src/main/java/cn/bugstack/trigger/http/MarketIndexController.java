package cn.bugstack.trigger.http;

import cn.bugstack.api.IMarketIndexService;
import cn.bugstack.api.dto.GoodsMarketRequestDTO;
import cn.bugstack.api.dto.GoodsMarketResponseDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.activity.model.entity.MarketProductEntity;
import cn.bugstack.domain.activity.model.entity.TrialBalanceEntity;
import cn.bugstack.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.model.valobj.TeamStatisticVO;
import cn.bugstack.domain.activity.service.IIndexGroupBuyMarketService;
import cn.bugstack.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/index/")
public class MarketIndexController implements IMarketIndexService {
    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;

    @RequestMapping(value = "query_group_buy_market_config", method = RequestMethod.POST)
    @Override
    public Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(@RequestBody  GoodsMarketRequestDTO requestDTO) throws Exception {
        log.info("查询拼团营销配置开始:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId());

        if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getGoodsId())) {
            return Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                    .build();
        }

        //1. 查询试算结果
        TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                .userId(requestDTO.getUserId())
                .source(requestDTO.getSource())
                .channel(requestDTO.getChannel())
                .goodsId(requestDTO.getGoodsId())
                .build());
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = trialBalanceEntity.getGroupBuyActivityDiscountVO();
        Long activityId = groupBuyActivityDiscountVO.getActivityId();

        //2.查询拼团组队
        List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities  =indexGroupBuyMarketService.queryInProgressUserGroupBuyOrderDetailList(activityId, requestDTO.getUserId(),1,2);

        //3.统计拼团数据

        TeamStatisticVO teamStatisticVO=indexGroupBuyMarketService.queryTeamStatisticByActivityId(activityId);

        //4.组装
        GoodsMarketResponseDTO.Goods goods = GoodsMarketResponseDTO.Goods.builder()
                .goodsId(trialBalanceEntity.getGoodsId())
                .originalPrice(trialBalanceEntity.getOriginalPrice())
                .deductionPrice(trialBalanceEntity.getDeductionPrice())
                .payPrice(trialBalanceEntity.getPayPrice())
                .build();

        List<GoodsMarketResponseDTO.Team> teams = new ArrayList<>();
        for(UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity:userGroupBuyOrderDetailEntities){
            GoodsMarketResponseDTO.Team team = GoodsMarketResponseDTO.Team.builder()
                    .userId(userGroupBuyOrderDetailEntity.getUserId())
                    .teamId(userGroupBuyOrderDetailEntity.getTeamId())
                    .activityId(userGroupBuyOrderDetailEntity.getActivityId())
                    .targetCount(userGroupBuyOrderDetailEntity.getTargetCount())
                    .completeCount(userGroupBuyOrderDetailEntity.getCompleteCount())
                    .lockCount(userGroupBuyOrderDetailEntity.getLockCount())
                    .validStartTime(userGroupBuyOrderDetailEntity.getValidStartTime())
                    .validEndTime(userGroupBuyOrderDetailEntity.getValidEndTime())
                    .build();
            teams.add(team);
        }

        GoodsMarketResponseDTO.TeamStatistic teamStatistic = GoodsMarketResponseDTO.TeamStatistic.builder()
                .allTeamCount(teamStatisticVO.getAllTeamCount())
                .allTeamCompleteCount(teamStatisticVO.getAllTeamCompleteCount())
                .allTeamUserCount(teamStatisticVO.getAllTeamUserCount())
                .build();

        GoodsMarketResponseDTO responseDTO = GoodsMarketResponseDTO.builder()
                .goods(goods)
                .teamList(teams)
                .teamStatistic(teamStatistic)
                .build();

        Response<GoodsMarketResponseDTO> response = Response.<GoodsMarketResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(responseDTO)
                .build();
        return response;
    }
}
