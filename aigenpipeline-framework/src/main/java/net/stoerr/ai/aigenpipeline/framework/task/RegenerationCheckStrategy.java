package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A strategy to decide whether we need to regenerate an output file from input files.
 */
public interface RegenerationCheckStrategy {

    /**
     * Decides whether the output file needs to be regenerated.
     *
     * @param output the output file
     * @param inputs the input files . Caution: a file with name "-" is a placeholder for stdin - it is not a file.
     * @return true if the output file needs to be regenerated.
     */
    boolean needsRegeneration(@Nonnull File output, @Nonnull List<File> inputs);

    final RegenerationCheckStrategy ALWAYS = (output, inputs) -> true;

    final RegenerationCheckStrategy IF_NOT_EXISTS = (output, inputs) -> !output.exists();

    /**
     * Regenerate when output file does not exist or is older than one of the input files.
     */
    final RegenerationCheckStrategy IF_OLDER = (output, inputs) -> {
        if (!output.exists()) return true;
        long outputTime = output.lastModified();
        for (File input : inputs) {
            if (input.getName().equals("-")) continue;
            if (input.lastModified() > outputTime) return true;
        }
        return false;
    };

    final RegenerationCheckStrategy VERSIONMARKER = (output, inputs) -> {
        throw new UnsupportedOperationException("Not implemented yet."); // FIXME hps 24/04/27 not implemented
    };

    static final Map<String, RegenerationCheckStrategy> STRATEGIES = Map.of(
            "a", ALWAYS,
            "n", IF_NOT_EXISTS,
            "o", IF_OLDER,
            "v", VERSIONMARKER
    );

}
