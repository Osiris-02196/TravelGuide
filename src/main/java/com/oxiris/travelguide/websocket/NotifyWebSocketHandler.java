package com.oxiris.travelguide.websocket;

import cn.hutool.json.JSONUtil;
import com.oxiris.travelguide.model.entity.Notify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息通知 WebSocket 处理器。
 * 连接地址: /ws/notify/{userId}
 */
@Component
@Slf4j
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    /**
     * userId -> WebSocketSession 的映射，用于点对点推送
     */
    private static final Map<Long, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立后回调
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Step 1: 从 URI 中提取 userId
        Long userId = extractUserId(session);
        if (userId == null) {
            log.warn("WebSocket 连接缺少 userId，关闭连接: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        // Step 2: 将 session 存入映射表
        SESSION_MAP.put(userId, session);
        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 连接关闭后回调
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Step 1: 从 URI 中提取 userId
        Long userId = extractUserId(session);
        if (userId != null) {
            // Step 2: 从映射表中移除
            SESSION_MAP.remove(userId);
            log.info("WebSocket 连接关闭: userId={}, sessionId={}, status={}", userId, session.getId(), status);
        }
    }

    /**
     * 传输发生异常时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输异常: sessionId={}, error={}", session.getId(), exception.getMessage());
        // Step 1: 尝试关闭连接
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.error("关闭 WebSocket 连接失败: {}", e.getMessage());
        }
        // Step 2: 从映射表中移除
        Long userId = extractUserId(session);
        if (userId != null) {
            SESSION_MAP.remove(userId);
        }
    }

    /**
     * 实时推送新消息
     *
     * @param notify 消息实体
     */
    public void pushNotify(Notify notify) {
        if (notify == null || notify.getReceiverId() == null) {
            return;
        }
        // Step 1: 构建推送 JSON
        Map<String, Object> data = Map.of(
                "id", notify.getId() != null ? notify.getId() : 0,
                "type", notify.getType() != null ? notify.getType() : "",
                "content", notify.getContent() != null ? notify.getContent() : "",
                "isRead", notify.getIsRead() != null ? notify.getIsRead() : 0,
                "createTime", notify.getCreateTime() != null ? notify.getCreateTime().toString() : ""
        );
        String jsonStr = JSONUtil.toJsonStr(data);
        // Step 2: 获取目标 session
        WebSocketSession session = SESSION_MAP.get(notify.getReceiverId());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonStr));
                log.info("WebSocket 推送消息成功: receiverId={}, notifyId={}", notify.getReceiverId(), notify.getId());
            } catch (IOException e) {
                log.error("WebSocket 推送消息失败: receiverId={}, error={}", notify.getReceiverId(), e.getMessage());
                // Step 3: 推送失败则清理无效 session
                SESSION_MAP.remove(notify.getReceiverId());
                try {
                    session.close();
                } catch (IOException ignored) {
                }
            }
        } else {
            log.debug("用户未连接 WebSocket，跳过实时推送: receiverId={}", notify.getReceiverId());
        }
    }

    /**
     * 推送未读数量更新
     *
     * @param receiverId  接收者ID
     * @param unreadCount 未读数量
     */
    public void pushUnreadCountUpdate(Long receiverId, Long unreadCount) {
        if (receiverId == null) {
            return;
        }
        // Step 1: 构建推送 JSON
        Map<String, Object> data = Map.of(
                "type", "unreadCount",
                "unreadCount", unreadCount != null ? unreadCount : 0
        );
        String jsonStr = JSONUtil.toJsonStr(data);
        // Step 2: 获取目标 session
        WebSocketSession session = SESSION_MAP.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonStr));
                log.info("推送未读数量更新成功: receiverId={}, unreadCount={}", receiverId, unreadCount);
            } catch (IOException e) {
                log.error("推送未读数量更新失败: receiverId={}, error={}", receiverId, e.getMessage());
                SESSION_MAP.remove(receiverId);
                try {
                    session.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 从 WebSocketSession URI 中提取 userId
     *
     * @param session WebSocket 会话
     * @return userId，提取失败返回 null
     */
    private Long extractUserId(WebSocketSession session) {
        try {
            String path = session.getUri().getPath();
            // 路径格式: /ws/notify/{userId}
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                return Long.valueOf(parts[3]);
            }
        } catch (Exception e) {
            log.error("从URI提取userId失败: {}", e.getMessage());
        }
        return null;
    }
}