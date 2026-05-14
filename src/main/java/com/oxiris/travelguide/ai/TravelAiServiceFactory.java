package com.oxiris.travelguide.ai;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TravelAiServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 每次调用创建一个新的 AI 服务（无缓存）
     */
    /**
     * 每次调用创建一个新的 AI 服务（无缓存）
     */
    public TravelAiService createService(String sessionId) {
        return AiServices.builder(TravelAiService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build())
                .build();
    }

}
