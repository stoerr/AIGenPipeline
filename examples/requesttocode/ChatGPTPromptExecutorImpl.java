// AIGenVersion(22608f97, codeRules.prompt-5223a176, createcode.prompt-7b58c524, ChatGPTPromptExecutorImpl.java-270eeb6e, ChatGPTPromptExecutor.java-7738d948, request.json-2f61881f, response.json-6105b6cb)

package net.stoerr.ai.aigenpipeline;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Implementation of {@link ChatGPTPromptExecutor} that sends a JSON payload to the OpenAI API.
 */
public class ChatGPTPromptExecutorImpl implements ChatGPTPromptExecutor {

    private static final String MODEL = "gpt-3.5-turbo";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String APPLICATION_JSON = "application/json";
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Constructs a new instance of {@code ChatGPTPromptExecutorImpl}.
     */
    public ChatGPTPromptExecutorImpl() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Sends a prompt together with a system message to ChatGPT and returns the response content.
     *
     * @param systemMessage The system message to send to ChatGPT.
     * @param prompt        The prompt to send to ChatGPT.
     * @return The content of the response from ChatGPT.
     * @throws IOException If an error occurs during the request.
     */
    @Override
    public String execute(String systemMessage, String prompt) throws IOException {
        RequestPayload payload = new RequestPayload(MODEL, List.of(
                new Message("system", systemMessage),
                new Message("user", prompt)
        ));

        String requestBody = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + OPENAI_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ResponsePayload responsePayload = gson.fromJson(response.body(), ResponsePayload.class);
            return responsePayload.choices.get(0).message.content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to OpenAI API was interrupted", e);
        }
    }

    /**
     * Represents the JSON payload to be sent to the OpenAI API.
     */
    private static class RequestPayload {
        final String model;
        final List<Message> messages;

        RequestPayload(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    /**
     * Represents a message within the JSON payload.
     */
    private static class Message {
        final String role;
        final String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    /**
     * Represents the JSON response payload from the OpenAI API.
     */
    private static class ResponsePayload {
        final List<Choice> choices;

        ResponsePayload(List<Choice> choices) {
            this.choices = choices;
        }
    }

    /**
     * Represents a choice within the JSON response payload.
     */
    private static class Choice {
        final Message message;

        Choice(Message message) {
            this.message = message;
        }
    }

    /**
     * Main method for testing the {@code ChatGPTPromptExecutorImpl}.
     *
     * @param args Command line arguments where args[0] is the system message and args[1] is the user prompt.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ChatGPTPromptExecutorImpl <systemMessage> <prompt>");
            return;
        }
        String systemMessage = args[0];
        String prompt = args[1];

        ChatGPTPromptExecutorImpl executor = new ChatGPTPromptExecutorImpl();
        try {
            String response = executor.execute(systemMessage, prompt);
            System.out.println("Response from ChatGPT: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
