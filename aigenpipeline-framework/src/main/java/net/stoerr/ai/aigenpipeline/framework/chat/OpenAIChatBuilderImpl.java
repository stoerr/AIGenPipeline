package net.stoerr.ai.aigenpipeline.framework.chat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Implementation of AIChatBuilder for creating and executing OpenAI chat completion requests.
 */
public class OpenAIChatBuilderImpl implements AIChatBuilder {

    public static final int DEFAULT_MAX_TOKENS = 2048;

    protected static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    protected final Pattern CODEBLOCK_PATTERN = Pattern.compile("\\A```\\w*\\n?(.*?)\\n*```\\Z", Pattern.DOTALL);

    protected String model = "gpt-4-turbo-preview";
    protected final List<Message> messages = new ArrayList<>();
    protected String openAiApiKey;
    protected int maxTokens = DEFAULT_MAX_TOKENS;
    protected String url = CHAT_COMPLETION_URL;

    public OpenAIChatBuilderImpl() {
        openAiApiKey = System.getenv("OPENAI_API_KEY");
    }

    @Override
    public AIChatBuilder url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public AIChatBuilder key(String key) {
        this.openAiApiKey = key;
        return this;
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
                .uri(URI.create(url))
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
            throw new IllegalStateException("Interrupted while waiting for chat completion response", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute chat completion request", e);
        }
    }

    public String toJson() {
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages, 0, maxTokens);
        return gson.toJson(request);
    }

    protected String extractResponse(String json) {
        ChatCompletionResponse response = gson.fromJson(json, ChatCompletionResponse.class);
        if (response.choices.isEmpty()) {
            throw new IllegalStateException("No choices in response: " + json);
        }
        ChatCompletionResponse.Choice choice = response.choices.get(0);
        if (!"stop".equals(choice.finish_reason)) {
            String msg = "Invalid finish reason: " + choice.finish_reason + " in response: " + json;
            if ("length".equals(choice.finish_reason)) {
                msg += "\n- try increasing maxTokens";
            }
            throw new IllegalStateException(msg);
        }
        String content = choice.message.content.trim();
        Matcher matcher = CODEBLOCK_PATTERN.matcher(content);
        if (matcher.matches()) {
            content = matcher.group(1);
        }
        return content;
    }

    protected static class Message {
        String role;
        String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    protected static class ChatCompletionRequest {
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

    protected static class ChatCompletionResponse {
        List<Choice> choices;

        static class Choice {
            Message message;
            String finish_reason;
        }
    }
}
