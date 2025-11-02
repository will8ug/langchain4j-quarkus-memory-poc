package io.will.langchain4jdemo;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@SystemMessage("You are a helpful assistant")
@ApplicationScoped
public interface AiAssistant {
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
