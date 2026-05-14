package com.oxiris.travelguide.controller;

import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.entity.AiChatSession;
import com.oxiris.travelguide.service.AiChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai/session")
public class AiChatSessionController {

    @Resource
    private AiChatSessionService aiChatSessionService;

    /**
     * 获取用户的所有 AI 会话列表
     */
    @GetMapping("/list")
    public BaseResponse<List<AiChatSession>> listSessions(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        List<AiChatSession> list = aiChatSessionService.listByUserId(userId);
        return ResultUtils.success(list);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/create")
    public BaseResponse<AiChatSession> createSession(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        String sessionId = UUID.randomUUID().toString();
        aiChatSessionService.createSession(userId, sessionId, "新的对话");
        AiChatSession session = aiChatSessionService.getBySessionId(sessionId);
        return ResultUtils.success(session);
    }

    /**
     * 删除会话（逻辑删除）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSession(@RequestParam Long id, @RequestParam Long userId) {
        ThrowUtils.throwIf(id == null || userId == null, ErrorCode.PARAMS_ERROR);
        boolean success = aiChatSessionService.deleteSession(id, userId);
        return ResultUtils.success(success);
    }
}