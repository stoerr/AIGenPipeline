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
 * That does work for interfaces similar to OpenAI chat completion - e.g. Anthropic Claude ,
 * local models with LM Studio , probably more.
 *
 * @see "https://platform.openai.com/docs/api-reference/chat"
 * @see "https://docs.anthropic.com/claude/reference/messages_post"
 */
public class OpenAIChatBuilderImpl implements AIChatBuilder {

    /** Environment variable for the Anthropic API version. */
    public static final String ENV_ANTHROPIC_VERSION = "ANTHROPIC_API_VERSION";
    /** Environment variable for the OpenAI API key. */
    public static final String ENV_OPENAI_API_KEY = "OPENAI_API_KEY";
    /** Environment variable for the Anthropic API key. */
    public static final String ENV_ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY";
    /** Default version for the Anthropic API, of not overridden with environment variable {@link #ENV_ANTHROPIC_VERSION}  . */
    public static final String ANTHROPIC_DEFAULT_VERSION = "2023-06-01";

    public static final int DEFAULT_MAX_TOKENS = 2048;

    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    protected final Pattern CODEBLOCK_PATTERN = Pattern.compile("\\A\\s*```\\w*\\n?(.*?)\\n*```\\s*\\Z", Pattern.DOTALL);

    protected String model = "gpt-4-turbo-preview";
    protected final List<Message> messages = new ArrayList<>();
    protected String apiKey;
    protected int maxTokens = DEFAULT_MAX_TOKENS;
    protected String url = AIModelConstants.OPENAI_URL;

    @Override
    public AIChatBuilder url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public AIChatBuilder key(String key) {
        this.apiKey = key;
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
            messages.add(0, new Message(ROLE_SYSTEM, text));
        }
        return this;
    }

    @Override
    public AIChatBuilder userMsg(String text) {
        if (text != null && !text.isEmpty()) {
            messages.add(new Message(ROLE_USER, text));
        }
        return this;
    }

    @Override
    public AIChatBuilder assistantMsg(String text) {
        if (text != null && !text.isEmpty()) {
            messages.add(new Message(ROLE_ASSISTANT, text));
        }
        return this;
    }

    /**
     * This returns {@link #apiKey} if given, otherwise resorts to environment variables depending on the {@link #url}
     */
    protected String determineApiKey() {
        String key = apiKey;
        if (key == null) {
            if (isOpenAI()) {
                key = System.getenv(ENV_OPENAI_API_KEY);
            } else if (isClaude()) {
                key = System.getenv(ENV_ANTHROPIC_API_KEY);
            }
        }
        return key;
    }

    protected boolean isClaude() {
        return url.contains("//api.anthropic.com/");
    }

    protected boolean isOpenAI() {
        return url.contains("//api.openai.com/");
    }

    @Override
    public String execute() {
        String key = determineApiKey();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson()));
        if (isOpenAI()) {
            builder.header("Authorization", "Bearer " + key);
        } else if (isClaude()) {
            // https://docs.anthropic.com/claude/reference/versions
            String anthropicVersion = System.getenv(ENV_ANTHROPIC_VERSION);
            anthropicVersion = anthropicVersion != null ? anthropicVersion : ANTHROPIC_DEFAULT_VERSION;
            builder.header("x-api-key", determineApiKey())
                    .header("anthropic-version", anthropicVersion);
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status code " + response.statusCode() + " : " + response.body());
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
        ChatCompletionRequest request;
        if (isClaude()) { // the system prompt for Claude is a request attribute, not a message.
            StringBuilder systemMessage = new StringBuilder();
            List<Message> filteredMessages = new ArrayList<>();
            for (Message message : messages) {
                if (ROLE_ASSISTANT.equals(message.role) || ROLE_USER.equals(message.role)) {
                    filteredMessages.add(message);
                } else if (ROLE_SYSTEM.equals(message.role)) {
                    if (systemMessage.length() > 0) {
                        systemMessage.append("\n\n");
                    }
                    systemMessage.append(message.content);
                } else {
                    throw new IllegalArgumentException("Unknown role " + message.role);
                }
            }
            request = new ChatCompletionRequest(model, filteredMessages, 0, maxTokens);
            if (systemMessage.length() > 0) {
                request.system = systemMessage.toString();
            }
        } else { // OpenAI format
            request = new ChatCompletionRequest(model, messages, 0, maxTokens);
        }
        return gson.toJson(request);
    }

    protected String extractResponse(String json) {
        ChatCompletionResponse response = gson.fromJson(json, ChatCompletionResponse.class);
        boolean stopped = false;
        String finish_reason = null;
        String content = null;
        if (response.choices != null && !response.choices.isEmpty()) { // OpenAI format
            ChatCompletionResponse.Choice choice = response.choices.get(0);
            content = choice.message.content.trim();
            finish_reason = choice.finish_reason;
            stopped = "stop".equals(finish_reason);

        } else if (response.content != null && !response.content.isEmpty()) { // Anthropic Claude
            finish_reason = response.stop_reason;
            stopped = "end_turn".equals(finish_reason);
            content = response.content.get(0).text;
        } else {
            throw new IllegalStateException("Could not find answer in response: " + json);
        }
        if (!stopped) {
            String msg = "Invalid finish reason: " + finish_reason + " in response: " + json;
            if (finish_reason != null && finish_reason.contains("length")) {
                msg += "\n- try increasing maxTokens";
            }
            throw new IllegalStateException(msg);
        }
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
        String system; // only for Anthropic Claude

        ChatCompletionRequest(String model, List<Message> messages, double temperature, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = maxTokens;
        }
    }

    protected static class ChatCompletionResponse {
        // OpenAI style response
        List<Choice> choices;

        // Claude style response
        List<ClaudeResponseContent> content;
        String stop_reason;

        static class Choice {
            Message message;
            String finish_reason;
        }

        static class ClaudeResponseContent {
            String type;
            String text;
        }
    }
}
