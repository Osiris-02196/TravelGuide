package com.oxiris.travelguide.service.impl;

import cn.hutool.core.lang.UUID;
import com.oxiris.travelguide.model.entity.AiChatSession;
import com.oxiris.travelguide.service.AiChatSessionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiChatSessionServiceImplTest {

    @Resource
    private AiChatSessionService aiChatSessionService;

    @Test
    void testCreateAndQuerySession() {

        Long userId = 1L;
        String sessionId = "test-" + UUID.randomUUID();
        String title = "东京旅游攻略";

        // 1️⃣ 创建会话
        aiChatSessionService.createSession(userId, sessionId, title);

        // 2️⃣ 查询会话
        AiChatSession session = aiChatSessionService.getBySessionId(sessionId);

        assert session != null;
        assert session.getUserId().equals(userId);
        assert session.getTitle().equals(title);

        System.out.println("查询成功：" + session);
    }

    @Test
    void testUpdateSessionTime() throws InterruptedException {

        Long userId = 1L;
        String sessionId = "test-" + UUID.randomUUID();

        // 创建
        aiChatSessionService.createSession(userId, sessionId, "测试更新");

        AiChatSession session = aiChatSessionService.getBySessionId(sessionId);
        assert session != null;

        // 等1秒，确保时间变化明显
        Thread.sleep(1000);

        // 3️⃣ 更新更新时间
        aiChatSessionService.updateSessionTime(session.getId());

        AiChatSession updated = aiChatSessionService.getBySessionId(sessionId);

        assert updated.getUpdateTime().isAfter(session.getUpdateTime());

        System.out.println("更新时间成功：" + updated.getUpdateTime());
    }
}