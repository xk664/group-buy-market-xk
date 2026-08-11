package cn.bugstack.ai.trigger.http;

import cn.bugstack.ai.chat.ChatRequest;
import cn.bugstack.ai.chat.ChatResponse;
import cn.bugstack.ai.chat.ChatService;
import cn.bugstack.api.response.Response;
import cn.bugstack.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * AI 客服对话接口（单轮）
 * POST /api/ai/chat
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/ai/")
public class AIChatController {

    @Resource
    private ChatService chatService;

    @PostMapping("chat")
    public Response<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.chat(request.getUserId(), request.getQuestion(), request.getSessionId());
            return Response.<ChatResponse>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(response)
                    .build();
        } catch (Exception e) {
            log.error("AI 客服问答失败 userId={} question={}", request.getUserId(), request.getQuestion(), e);
            return Response.<ChatResponse>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}