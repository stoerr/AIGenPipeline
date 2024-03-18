package net.stoerr.ai.aigenpipeline.framework.chat;

import java.util.SortedMap;

/**
 * Executes all chat types for a quick check everything works. Since this accesses the net and requires a local model
 * to run this is not a unit test.
 */
public class RunAllChats {

    public static void main(String[] args) {
        System.out.println("\n\nAnthropic");
        RunAnthropicChat.main(args);
        System.out.println("\n\nOpenAI");
        RunOpenAIChat.main(args);
        System.out.println("\n\nLocal LM Studio");
        RunLocalChat.main(args);
    }
}
