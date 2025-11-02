package io.will.langchain4jdemo;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CompressionChatMemoryProvider implements ChatMemoryProvider {
    @Inject
    CompressingChatMemoryStore compressionChatMemoryStore;

    /*public CompressionChatMemoryProvider(ChatModel chatModel) {
        compressionChatMemoryStore = new CompressingChatMemoryStore(chatModel, 5);
    }*/

    @Override
    public ChatMemory get(Object memoryId) {
        return CompressionChatMemory.builder()
                .memoryId(memoryId)
                .chatMemoryStore(compressionChatMemoryStore)
                .build();
    }
}
