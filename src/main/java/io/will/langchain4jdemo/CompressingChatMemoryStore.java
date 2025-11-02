package io.will.langchain4jdemo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CompressingChatMemoryStore implements ChatMemoryStore {
    private final ChatMemoryStore delegate;
    private final ChatModel chatModel;
    private final int threshold;

    private static final String SUMMARY_PREFIX = "Context: The following is a summary of the previous conversation:";

    public CompressingChatMemoryStore(
            ChatModel chatModel,
            @ConfigProperty(name = "semantic-compression-threshold", defaultValue = "5") int threshold) {
        this.delegate = new InMemoryChatMemoryStore();
        this.chatModel = chatModel;
        this.threshold = threshold;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Log.infof("Updating memory ID: %s, %s", memoryId, messages.toString());
        if (messages.isEmpty()) {
            Log.warnf("No messages to compress for memory ID: %s", memoryId);
            return;
        }

       ChatMessage lastMsg = messages.getLast();
       if (lastMsg.type() == ChatMessageType.AI && ((AiMessage) lastMsg).hasToolExecutionRequests()) {
           Log.infof("Skipping compression for memory ID: %s due to function call in the last message", memoryId);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       if (lastMsg.type() == ChatMessageType.SYSTEM || lastMsg.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
           Log.infof(
                   "Skipping compression for memory ID: %s due to system message or function call response in the last message",
                   memoryId);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       if (messages.size() <= threshold) {
           Log.debugf("No compression for memory ID: %s due to less than %d messages", memoryId, threshold);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       String summary = compressMessages(memoryId, messages);

       SystemMessage systemMsg = (SystemMessage) messages.stream()
               .filter(m -> m.type() == ChatMessageType.SYSTEM)
               .findFirst().orElse(null);
       systemMsg = replaceTheLatestSummary(systemMsg, summary);
       Log.infof("Generated system message with summary: %s", systemMsg.text());
       Log.infof("Updating memory messages of memory ID: %s", memoryId);
       delegate.updateMessages(memoryId, List.of(systemMsg));
        // delegate.updateMessages(memoryId, messages);
    }

    private String compressMessages(Object memoryId, List<ChatMessage> messages) {
        Log.infof("Triggering semantic compression for memory ID: %s with %d messages", memoryId, messages.size());
        List<ChatMessage> toBeCompressed = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.type() == ChatMessageType.SYSTEM) {
                extractSummaryFromSystemMessageIfAny((SystemMessage) msg, toBeCompressed);
            } else {
                toBeCompressed.add(msg);
            }
        }

        StringBuilder sb = new StringBuilder("""
                Summarize the following dialogue into a brief summary, preserving context and tone:
                
                """);
        for (ChatMessage msg : toBeCompressed) {
            switch (msg.type()) {
                case ChatMessageType.SYSTEM -> sb.append("Context: ").append(((SystemMessage) msg).text()).append("\n");
                case ChatMessageType.USER -> sb.append("User: ").append(((UserMessage) msg).singleText()).append("\n");
                case ChatMessageType.AI -> sb.append("Assistant: ").append(((AiMessage) msg).text()).append("\n");
                default -> Log.debugf("Skipping message of type: %s", msg.type());
            }
        }
        return chatModel.chat(sb.toString());
    }

    private void extractSummaryFromSystemMessageIfAny(SystemMessage systemMsg, List<ChatMessage> compressed) {
        String content = systemMsg.text();
        if (content.contains(SUMMARY_PREFIX)) {
            int startIndex = content.indexOf(SUMMARY_PREFIX) + SUMMARY_PREFIX.length();
            String summary = content.substring(startIndex).strip();
            compressed.add(SystemMessage.systemMessage(summary));
        }
    }

    private SystemMessage replaceTheLatestSummary(SystemMessage systemMsg, String summary) {
        if (systemMsg == null) {
            return SystemMessage.systemMessage(SUMMARY_PREFIX + "\n" + summary);
        }

        String content = systemMsg.text();
        String newContent = "";
        if (content.contains(SUMMARY_PREFIX)) {
            int startIndex = content.indexOf(SUMMARY_PREFIX);
            if (startIndex > 0) {
                newContent = content.substring(0, startIndex) + "\n\n";
            }
            newContent = newContent + SUMMARY_PREFIX + "\n" + summary;
        } else {
            newContent = content + "\n\n" + SUMMARY_PREFIX + "\n" + summary;
        }
        return SystemMessage.systemMessage(newContent);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> currentMessages = delegate.getMessages(memoryId);
        Log.infof("getMessages memory ID: %s, %s", memoryId, currentMessages);
        return currentMessages;
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Log.infof("Current messages: %s", delegate.getMessages(memoryId));
        Log.infof("Deleting memory ID: %s", memoryId);
        Thread.dumpStack();
        delegate.deleteMessages(memoryId);
    }
}
