package net.stoerr.ai.aigenpipeline.framework.task;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;

/**
 * Mock implementation of AIChatBuilder for testing purposes.
 */
public class MockAIChatBuilder extends OpenAIChatBuilderImpl {

    @Override
    public String execute() {
        // FIXME must not appear there since that'd simulate an error message of the AI.
        return "Response to:\n" + toJson().replaceAll("FIXME", "EMCIF");
    }

}
