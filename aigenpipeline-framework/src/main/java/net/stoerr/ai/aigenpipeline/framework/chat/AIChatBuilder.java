package net.stoerr.ai.aigenpipeline.framework.chat;

/**
 * Defines the interface for building AI chat interactions, allowing customization of the model, token limits, and messages.
 */
public interface AIChatBuilder {

    /**
     * Sets the maximum number of tokens the completion can use.
     * @param maxTokens The maximum number of tokens.
     * @return The builder instance for chaining.
     */
    AIChatBuilder maxTokens(int maxTokens);

    /**
     * Sets the AI model to be used for the chat completion.
     * @param model The model name.
     * @return The builder instance for chaining.
     */
    AIChatBuilder model(String model);

    /**
     * Adds a system message to the chat.
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder systemMsg(String text);

    /**
     * Adds a user message to the chat.
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder userMsg(String text);

    /**
     * Adds an assistant message to the chat.
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder assistantMsg(String text);

    /**
     * Converts the chat completion request to a JSON string.
     * @return The JSON string.
     */
    String toJson();

    /**
     * Executes the chat completion request and returns the response.
     * @return The chat completion response.
     */
    String execute();
}
