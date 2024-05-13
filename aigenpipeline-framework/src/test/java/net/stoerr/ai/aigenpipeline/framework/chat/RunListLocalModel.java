package net.stoerr.ai.aigenpipeline.framework.chat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Strangely, connections to LM Studio block for an unknown reason. That seems to be either an issue with LM Studio
 * or the java.net.http.HttpClient. If I use the burpsuite as proxy, it works.
 */
public class RunListLocalModel {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello, World!");
        String url = "http://localhost:1234/v1/chat/completions";
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(java.time.Duration.ofSeconds(20))
                // .proxy(ProxySelector.of(InetSocketAddress.createUnresolved("localhost", 8080)))
                .build();
        String json = "{\n" +
                "  \"model\": \"gpt-3.5-turbo\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"Print what technology you're based on, as a short identifier. Print only the manufacturer and model identifier, no other text.\\n\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"temperature\": 0.0,\n" +
                "  \"max_tokens\": 2048\n" +
                "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(40))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse.BodyHandler<String> bodyHandler = (rspInfo) -> {
            System.out.println("Response Info: " + rspInfo.statusCode());
            return HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        };
        // HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpResponse response = client.send(request, bodyHandler);
        System.out.println("Response Code: " + response.statusCode());
        System.out.flush();
        System.out.printf("Response: %s%n", response.body());
    }
}

/*
curl http://localhost:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "TheBloke/Mistral-7B-Instruct-v0.2-GGUF",
    "messages": [
      { "role": "system", "content": "Always answer in rhymes." },
      { "role": "user", "content": "Introduce yourself." }
    ],
    "temperature": 0.7,
    "max_tokens": -1,
    "stream": false
}'

 */
