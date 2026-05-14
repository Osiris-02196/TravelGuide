package com.oxiris.travelguide.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.oxiris.travelguide.ai.TravelAiServiceFactory;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.mapper.AiChatMessageMapper;
import com.oxiris.travelguide.model.entity.AiChatMessage;
import com.oxiris.travelguide.model.entity.AiChatSession;
import com.oxiris.travelguide.service.AiChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class TravelAiController {

    @Resource
    private AiChatSessionService aiChatSessionService;

    @Resource
    private AiChatMessageMapper aiChatMessageMapper;

    private final TravelAiServiceFactory factory;

    public TravelAiController(TravelAiServiceFactory factory) {
        this.factory = factory;
    }

    @PostMapping("/chat")
    public String chat(@RequestParam String userId,
                       @RequestParam String message,
                       @RequestParam(required = false) String sessionId) {
        // 1. 查找或创建会话
        String actualSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : "user:" + userId;
        AiChatSession session = aiChatSessionService.getBySessionId(actualSessionId);
        if (session == null) {
            aiChatSessionService.createSession(Long.valueOf(userId), actualSessionId, "AI对话");
        } else {
            aiChatSessionService.updateSessionTime(session.getId());
        }

        // 2. 保存用户消息
        AiChatMessage userMsg = AiChatMessage.builder()
                .sessionId(actualSessionId)
                .role("user")
                .content(message)
                .createTime(LocalDateTime.now())
                .build();
        aiChatMessageMapper.insert(userMsg);

        // 3. 调用AI
        com.oxiris.travelguide.ai.TravelAiService service = factory.createService(userId);
        String aiResponse = service.chat(actualSessionId, message);

        // 4. 保存AI回复
        AiChatMessage aiMsg = AiChatMessage.builder()
                .sessionId(actualSessionId)
                .role("ai")
                .content(aiResponse)
                .createTime(LocalDateTime.now())
                .build();
        aiChatMessageMapper.insert(aiMsg);

        return aiResponse;
    }

    /**
     * 获取会话的消息列表
     */
    @GetMapping("/session/messages")
    public BaseResponse<List<AiChatMessage>> getSessionMessages(@RequestParam String sessionId) {
        List<AiChatMessage> messages = aiChatMessageMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("sessionId", sessionId)
                        .orderBy("createTime", true)
        );
        return ResultUtils.success(messages);
    }
}
