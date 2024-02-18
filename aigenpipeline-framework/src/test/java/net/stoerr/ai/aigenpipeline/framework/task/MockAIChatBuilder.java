package net.stoerr.ai.aigenpipeline.framework.task;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;

/**
 * Mock implementation of AIChatBuilder for testing purposes.
 */
public class MockAIChatBuilder extends OpenAIChatBuilderImpl {

    @Override
    public String execute() {
        return "Response to:\n" + toJson();
    }

}
