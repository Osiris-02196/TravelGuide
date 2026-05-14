package com.oxiris.travelguide.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.oxiris.travelguide.mapper.AiChatSessionMapper;
import com.oxiris.travelguide.model.entity.AiChatSession;
import com.oxiris.travelguide.service.AiChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatSessionServiceImpl implements AiChatSessionService {

    @Resource
    private AiChatSessionMapper aiChatSessionMapper;

    @Override
    public AiChatSession getBySessionId(String sessionId) {
        return aiChatSessionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("sessionId", sessionId)
                        .eq("isDelete", 0)
        );
    }

    @Override
    public void createSession(Long userId, String sessionId, String title) {
        AiChatSession session = AiChatSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .title(title)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        aiChatSessionMapper.insert(session);
    }

    @Override
    public void updateSessionTime(Long id) {
        AiChatSession session = new AiChatSession();
        session.setId(id);
        session.setUpdateTime(LocalDateTime.now());
        aiChatSessionMapper.updateByQuery(session,
                QueryWrapper.create().eq("id", id));
    }

    @Override
    public List<AiChatSession> listByUserId(Long userId) {
        return aiChatSessionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("userId", userId)
                        .eq("isDelete", 0)
                        .orderBy("updateTime", false) // false = desc
        );
    }

    @Override
    public boolean deleteSession(Long id, Long userId) {
        AiChatSession session = new AiChatSession();
        session.setId(id);
        session.setIsDelete(1);
        int affected = aiChatSessionMapper.updateByQuery(session,
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("userId", userId) // 只允许删除自己的会话
        );
        return affected > 0;
    }
}