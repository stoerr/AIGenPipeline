package net.stoerr.ai.aigenpipeline.framework.chat;

/**
 * Defines the interface for building AI chat interactions, allowing customization of the model, token limits, and messages.
 */
public interface AIChatBuilder {


    /**
     * Sets the URL for the chat completion request.
     *
     * @param url The URL.
     * @return The builder instance for chaining.
     */
    AIChatBuilder url(String url);

    /**
     * Sets the API key for the chat completion request.
     *
     * @param key The API key.
     * @return The builder instance for chaining.
     */
    AIChatBuilder key(String key);

    /**
     * Sets the organization ID for the chat completion request - if applicable (OpenAI).
     *
     * @param organizationId The organization ID.
     * @return The builder instance for chaining.
     */
    AIChatBuilder organizationId(String organizationId);

    /**
     * Sets the maximum number of tokens the completion can use.
     *
     * @param maxTokens The maximum number of tokens.
     * @return The builder instance for chaining.
     */
    AIChatBuilder maxTokens(int maxTokens);

    /**
     * Sets the AI model to be used for the chat completion.
     *
     * @param model The model name.
     * @return The builder instance for chaining.
     */
    AIChatBuilder model(String model);

    /**
     * Adds a system message to the chat.
     *
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder systemMsg(String text);

    /**
     * Adds a user message to the chat.
     *
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder userMsg(String text);

    /**
     * Adds an assistant message to the chat.
     *
     * @param text The text of the message.
     * @return The builder instance for chaining.
     */
    AIChatBuilder assistantMsg(String text);

    /**
     * Converts the chat completion request to a JSON string.
     *
     * @return The JSON string.
     */
    String toJson();

    /**
     * Executes the chat completion request and returns the response.
     *
     * @return The chat completion response.
     */
    String execute();
}
