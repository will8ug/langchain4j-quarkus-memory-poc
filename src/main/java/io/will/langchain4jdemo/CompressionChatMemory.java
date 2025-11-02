package io.will.langchain4jdemo;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

public class CompressionChatMemory implements ChatMemory {
    private final Object memoryId;
    private final ChatMemoryStore store;

    private CompressionChatMemory(Object memoryId, ChatMemoryStore store) {
        this.memoryId = memoryId;
        this.store = store;
    }

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = new ArrayList<>(messages());
        messages.add(message);
        store.updateMessages(memoryId, messages);
    }

    @Override
    public List<ChatMessage> messages() {
        return store.getMessages(memoryId);
    }

    @Override
    public void clear() {
        store.deleteMessages(memoryId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatMemoryStore store;
        private Object memoryId;

        private Builder() {}

        public Builder chatMemoryStore(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        public Builder memoryId(Object memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        public CompressionChatMemory build() {
            return new CompressionChatMemory(memoryId, store);
        }
    }
}
