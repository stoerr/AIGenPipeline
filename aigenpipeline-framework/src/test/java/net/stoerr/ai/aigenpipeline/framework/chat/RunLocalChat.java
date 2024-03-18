package net.stoerr.ai.aigenpipeline.framework.chat;

public class RunLocalChat {
    public static void main(String[] args) {
        AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();
        String response = chatBuilder
                // LM Studio default
                .url("http://localhost:1234/v1/chat/completions")
                // .systemMsg("Speak in haikus") // several sysmsg doesn't work.
                // .systemMsg("Be a helpful assistant")
                .systemMsg("Be a helpful assistant who always speaks in haikus")
                .userMsg("Hi")
                .execute();
        System.out.println(response);
    }
}
