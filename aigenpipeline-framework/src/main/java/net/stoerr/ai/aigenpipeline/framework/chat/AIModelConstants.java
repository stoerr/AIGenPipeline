package net.stoerr.ai.aigenpipeline.framework.chat;

public interface AIModelConstants {

    /**
     * OpenAI chat message completion URL - the default.
     */
    String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    String GPT_3_5_TURBO = "gpt-3.5-turbo";
    String GPT_4 = "gpt-4o";

    /**
     * Anthropic Claude message completion URL.
     * There is a wild number of models with version numbers, so we don't specify constants for them.
     *
     * @see "https://docs.anthropic.com/claude/docs/models-overview#model-comparison"
     */
    String CLAUDE_URL = "https://api.anthropic.com/v1/messages";

}
