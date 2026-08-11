package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.chat.FeedbackRequest;
import cn.bugstack.ai.rag.repository.FeedbackRepository;
import cn.bugstack.api.response.Response;
import cn.bugstack.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户反馈上报（点赞/点踩/转人工）
 * POST /api/ai/feedback
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/ai/")
public class AIFeedbackController {

    @Resource
    private FeedbackRepository feedbackRepository;

    @PostMapping("feedback")
    public Response<Boolean> feedback(@RequestBody FeedbackRequest request) {
        try {
            feedbackRepository.insert(request.getMessageId(), request.getUserId(),
                    request.getFeedbackType(), request.getContent());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(Boolean.TRUE)
                    .build();
        } catch (Exception e) {
            log.error("AI 反馈上报失败 messageId={}", request.getMessageId(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}