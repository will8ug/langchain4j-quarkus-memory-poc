package io.will.langchain4jdemo;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/")
public class ChatResource {
    @Inject
    AiAssistant aiAssistant;
    @Inject
    ChatMemoryStore chatMemoryStore;

    private static final String DEMO_ID = "demo-id";

    @POST
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse chat(ChatRequest request) {
        String response = aiAssistant.chat(DEMO_ID, request.query());
        return new ChatResponse(response);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "ok";
    }

    @GET
    @Path("/messages")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChatMessage> messages() {
        return chatMemoryStore.getMessages(DEMO_ID);
    }

    @GET
    @Path("/messages/{memoryId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ChatMessage> getMessages(@PathParam("memoryId") String memoryId) {
        return chatMemoryStore.getMessages(memoryId);
    }

    public record ChatRequest(String query) {
    }

    public record ChatResponse(String response) {
    }
}
