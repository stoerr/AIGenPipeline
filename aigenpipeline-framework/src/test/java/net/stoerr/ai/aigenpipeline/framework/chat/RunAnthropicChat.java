package net.stoerr.ai.aigenpipeline.framework.chat;

public class RunAnthropicChat {
    public static void main(String[] args) {
        AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();
        String response = chatBuilder
                .url(AIModelConstants.CLAUDE_URL)
                .model("claude-3-haiku-20240307")
                .userMsg("Hi")
                .systemMsg("Be a helpful assistant.")
                .systemMsg("IMPORTANT: always speak in haikus.")
                .execute();
        System.out.println(response);
    }
}
