package net.stoerr.ai.aigenpipeline.examples.requesttocode;

public interface ChatGPTPromptExecutor {

    /**
     * Sends a prompt together with a system message to ChatGPT and returns the response.
     *
     * @param systemmessage The system message to send to ChatGPT.
     * @param prompt        The prompt to send to ChatGPT.
     * @return The response from ChatGPT.
     * @throws IOException If an error occurs during the request.
     */
    String execute(String systemmessage, String prompt) throws IOException;

}
