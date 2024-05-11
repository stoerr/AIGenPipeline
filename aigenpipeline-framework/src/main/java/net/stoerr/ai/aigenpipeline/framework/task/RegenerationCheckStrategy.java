package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * A strategy to decide whether we need to regenerate an output file from input files.
 */
public interface RegenerationCheckStrategy {

    /**
     * Decides whether the output file needs to be regenerated.
     *
     * @param output          the output file
     * @param inputs          the input files . Caution: a file with name "-" is a placeholder for stdin - it is not a file.
     * @param writingStrategy the writing strategy used to write the output file.
     * @param inputVersions   versions of all inputs
     * @return true if the output file needs to be regenerated.
     */
    boolean needsRegeneration(@Nonnull AIInOut output, @Nonnull List<AIInOut> inputs,
                              @Nonnull WritingStrategy writingStrategy, @Nonnull List<String> inputVersions);

    RegenerationCheckStrategy ALWAYS = (output, inputs, writingStrategy, inputVersions) -> true;

    RegenerationCheckStrategy IF_NOT_EXISTS = (output, inputs, writingStrategy, additionalMarkers) -> !output.getFile().exists();

    /**
     * Regenerate when output file does not exist or is older than one of the input files.
     */
    RegenerationCheckStrategy IF_OLDER = (output, inputs, writingStrategy, inputVersions) -> {
        if (!output.getFile().exists()) return true;
        long outputTime = output.getFile().lastModified();
        for (AIInOut input : inputs) {
            if (input.getFile().toString().equals("-")) continue;
            if (input.getFile().lastModified() > outputTime) return true;
        }
        return false;
    };

    RegenerationCheckStrategy VERSIONMARKER = new VersionMarkerRegenerationCheckStrategy();

    public class VersionMarkerRegenerationCheckStrategy implements RegenerationCheckStrategy {

        @Override
        public boolean needsRegeneration(@Nonnull AIInOut output, @Nonnull List<AIInOut> inputs,
                                         @Nonnull WritingStrategy writingStrategy, @Nonnull List<String> inputVersions) {
            AIVersionMarker outputVersionMarker = writingStrategy.getRecordedVersionMarker(output);
            if (outputVersionMarker == null) {
                return true;
            }
            List<String> oldInputVersions = outputVersionMarker.getInputVersions();
            return !new HashSet<Object>(inputVersions).equals(new HashSet<Object>(oldInputVersions));
        }

    }

}
