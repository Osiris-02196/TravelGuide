package com.oxiris.travelguide.config;

import com.oxiris.travelguide.websocket.NotifyWebSocketHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类。
 * 注册 /ws/notify/{userId} 路径。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private NotifyWebSocketHandler notifyWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notifyWebSocketHandler, "/ws/notify/{userId}")
                .setAllowedOrigins("*");
    }
}