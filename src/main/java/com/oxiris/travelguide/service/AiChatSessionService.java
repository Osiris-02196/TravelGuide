package com.oxiris.travelguide.service;

import com.oxiris.travelguide.model.entity.AiChatSession;

import java.util.List;

public interface AiChatSessionService {

    AiChatSession getBySessionId(String sessionId);

    void createSession(Long userId, String sessionId, String title);

    void updateSessionTime(Long id);

    /**
     * 查询用户的所有会话列表，按更新时间降序
     */
    List<AiChatSession> listByUserId(Long userId);

    /**
     * 逻辑删除会话
     */
    boolean deleteSession(Long id, Long userId);
}