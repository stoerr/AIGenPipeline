package net.stoerr.ai.aigenpipeline.framework.chat;

public class RunOpenAIChat {
    public static void main(String[] args) {
        AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();
        String response = chatBuilder
                .url(AIModelConstants.OPENAI_URL)
                .model(AIModelConstants.GPT_3_5_TURBO)
                .userMsg("Hi")
                .systemMsg("Be a helpful assistant")
                .systemMsg("Speak in haikus")
                .execute();
        System.out.println(response);
    }
}
