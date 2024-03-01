// AIGenVersion(56af7fa3, codeRules.prompt-5223a176, createcode.prompt-5d0f9bca, ChatGPTPromptExecutorImpl.java-56af7fa3, ChatGPTPromptExecutor.java-7738d948, request.json-2f61881f, response.json-1c2114e7)

package net.stoerr.ai.aigenpipeline.examples.requesttocode;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Implementation of ChatGPTPromptExecutor that sends a JSON payload to OpenAI Chat API.
 */
public class OpenAIChatGPTPromptExecutor implements ChatGPTPromptExecutor {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CONTENT_TYPE = "application/json";
    private static final String AUTHORIZATION_HEADER = "Bearer " + System.getenv("OPENAI_API_KEY");

    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Constructor for OpenAIChatGPTPromptExecutor.
     */
    public OpenAIChatGPTPromptExecutor() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Sends a prompt together with a system message to OpenAI Chat API and returns the response.
     *
     * @param systemMessage The system message to send to OpenAI Chat API.
     * @param prompt        The prompt to send to OpenAI Chat API.
     * @return The response from OpenAI Chat API.
     * @throws IOException If an error occurs during the request.
     */
    @Override
    public String execute(String systemMessage, String prompt) throws IOException {
        ChatGPTRequest request = new ChatGPTRequest(systemMessage, prompt);
        String requestBody = gson.toJson(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", CONTENT_TYPE)
                .header("Authorization", AUTHORIZATION_HEADER)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Inner class representing the JSON payload for the request to OpenAI Chat API.
     */
    private static class ChatGPTRequest {
        private final String systemMessage;
        private final String prompt;

        public ChatGPTRequest(String systemMessage, String prompt) {
            this.systemMessage = systemMessage;
            this.prompt = prompt;
        }
    }

    /**
     * Inner class representing the JSON response from OpenAI Chat API.
     */
    private static class ChatGPTResponse {
        private String systemMessage;
        private String prompt;
        private String completion;
    }

    /**
     * Main method for testing the OpenAIChatGPTPromptExecutor.
     *
     * @param args The system message and prompt to test.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java OpenAIChatGPTPromptExecutor <systemMessage> <prompt>");
            return;
        }

        String systemMessage = args[0];
        String prompt = args[1];

        OpenAIChatGPTPromptExecutor executor = new OpenAIChatGPTPromptExecutor();
        try {
            String response = executor.execute(systemMessage, prompt);
            System.out.println("Response from OpenAI Chat API: " + response);
        } catch (IOException e) {
            System.err.println("Error executing request: " + e.getMessage());
        }
    }
}
