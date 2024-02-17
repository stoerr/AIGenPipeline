package net.stoerr.ai.aigenpipeline.framework.chat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Implementation of AIChatBuilder for creating and executing OpenAI chat completion requests.
 */
public class OpenAIChatBuilderImpl implements AIChatBuilder {

    private static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String model = "gpt-4-turbo-preview";
    private final List<Message> messages = new ArrayList<>();
    private String openAiApiKey;
    private int maxTokens = 1000;

    public OpenAIChatBuilderImpl() {
        openAiApiKey = System.getenv("OPENAI_API_KEY");
        Objects.requireNonNull(openAiApiKey, "openAiApiKey needed - not found in environment variable OPENAI_API_KEY");
    }

    @Override
    public AIChatBuilder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    @Override
    public AIChatBuilder model(String model) {
        this.model = model;
        return this;
    }

    @Override
    public AIChatBuilder systemMsg(String text) {
        if (text != null && !text.isEmpty()) {
            messages.add(new Message("system", text));
        }
        return this;
    }

    @Override
    public AIChatBuilder userMsg(String text) {
        if (text != null && !text.isEmpty()) {
            messages.add(new Message("user", text));
        }
        return this;
    }

    @Override
    public AIChatBuilder assistantMsg(String text) {
        if (text != null && !text.isEmpty()) {
            messages.add(new Message("assistant", text));
        }
        return this;
    }

    @Override
    public String execute() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_COMPLETION_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(toJson()))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status code " + response.statusCode() + " from OpenAI: " + response.body());
            }
            return extractResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for chat completion response", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute chat completion request", e);
        }
    }

    public String toJson() {
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, 0, maxTokens);
        return gson.toJson(request);
    }

    private String extractResponse(String json) {
        ChatCompletionResponse response = gson.fromJson(json, ChatCompletionResponse.class);
        if (response.choices.isEmpty()) {
            throw new IllegalStateException("No choices in response: " + json);
        }
        ChatCompletionResponse.Choice choice = response.choices.get(0);
        if (!"stop".equals(choice.finish_reason)) {
            throw new IllegalStateException("Invalid finish reason: " + choice.finish_reason + " in response: " + json);
        }
        return choice.message.content;
    }

    private static class Message {
        String role;
        String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatCompletionRequest {
        String model;
        List<Message> messages;
        double temperature;
        int max_tokens;

        ChatCompletionRequest(String model, List<Message> messages, double temperature, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = maxTokens;
        }
    }

    private static class ChatCompletionResponse {
        List<Choice> choices;

        static class Choice {
            Message message;
            String finish_reason;
        }
    }
}
