package net.stoerr.ai.aigenpipeline.framework.chat;

import static net.stoerr.ai.aigenpipeline.framework.task.AIGenerationTask.unclutter;

/**
 * A pseudo AI chat model that just copies the input to the output.
 */
public class CopyPseudoAIChatBuilderImpl implements AIChatBuilder {

    /**
     * Pseudo model that just concatenates the inputs and writes these to the output.
     */
    public static final String MODEL_COPY = "copy";

    protected final StringBuilder allInputs = new StringBuilder();

    @Override
    public AIChatBuilder url(String url) {
        // not used
        return this;
    }

    @Override
    public AIChatBuilder key(String key) {
        // not used
        return this;
    }

    @Override
    public AIChatBuilder organizationId(String organizationId) {
        // not used
        return this;
    }

    @Override
    public AIChatBuilder maxTokens(int maxTokens) {
        // not used
        return this;
    }

    @Override
    public AIChatBuilder model(String model) {
        if (!MODEL_COPY.equals(model)) {
            throw new IllegalArgumentException("Only model " + MODEL_COPY + " is supported.");
        }
        return this;
    }

    @Override
    public AIChatBuilder systemMsg(String text) {
        // not used
        return this;
    }

    @Override
    public AIChatBuilder userMsg(String text) {
        // not used
        return this;
    }

    /**
     * We assume the "put it into the mouth of the AI" pattern is used. Then all the assistant messages are actually
     * the inputs. (That is a bit dangerous if it's used differently, but the easiest way.)
     */
    @Override
    public AIChatBuilder assistantMsg(String text) {
        if (text != null) {
            allInputs.append(unclutter(text));
        }
        return this;
    }

    @Override
    public String toJson() {
        return allInputs.toString();
    }

    @Override
    public String execute() {
        return allInputs.toString();
    }
}
