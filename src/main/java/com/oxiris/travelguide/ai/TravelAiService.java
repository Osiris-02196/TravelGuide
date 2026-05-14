package com.oxiris.travelguide.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 旅游 AI 助手
 */
public interface TravelAiService {

    @SystemMessage(fromResource = "prompt/AiChat-prompt.txt")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
