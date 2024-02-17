package net.stoerr.ai.aigenpipeline.framework.chat;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class OpenAIChatBuilderImplTest {

    @Test
    public void testToJson() {
        OpenAIChatBuilderImpl chatBuilder = new OpenAIChatBuilderImpl();
        chatBuilder.model("test-model").maxTokens(500)
                .userMsg("Hello, world!")
                .assistantMsg("Hi there!");

        String actualJson = chatBuilder.toJson();
        Assert.assertEquals("{\n" +
                "  \"model\": \"test-model\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"Hello, world!\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"role\": \"assistant\",\n" +
                "      \"content\": \"Hi there!\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"temperature\": 0.0,\n" +
                "  \"max_tokens\": 500\n" +
                "}", actualJson);
    }

    /**
     * Works only if OPEN_API_KEY is set in the environment.
     */
    @Test
    @Ignore("Only run if OPEN_API_KEY is set in the environment and you want to try a real call.")
    public void testRealCall() {
        OpenAIChatBuilderImpl chatBuilder = new OpenAIChatBuilderImpl();
        chatBuilder.maxTokens(500)
                .userMsg("Say Hi!")
                .assistantMsg("Hi!")
                .userMsg("Say Hi again!");
        String response = chatBuilder.execute();
        Assert.assertEquals("Hi again!", response);
    }
}
