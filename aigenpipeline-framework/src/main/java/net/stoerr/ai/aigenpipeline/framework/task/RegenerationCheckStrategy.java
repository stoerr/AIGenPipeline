package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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
     * @param output            the output file
     * @param inputs            the input files . Caution: a file with name "-" is a placeholder for stdin - it is not a file.
     * @param writingStrategy   the writing strategy used to write the output file.
     * @param inputVersions     versions of all inputs
     * @return true if the output file needs to be regenerated.
     */
    boolean needsRegeneration(@Nonnull File output, @Nonnull List<File> inputs,
                              @Nonnull WritingStrategy writingStrategy, @Nonnull List<String> inputVersions) throws IOException;

    RegenerationCheckStrategy ALWAYS = (output, inputs, writingStrategy, inputVersions) -> true;

    RegenerationCheckStrategy IF_NOT_EXISTS = (output, inputs, writingStrategy, additionalMarkers) -> !output.exists();

    /**
     * Regenerate when output file does not exist or is older than one of the input files.
     */
    RegenerationCheckStrategy IF_OLDER = (output, inputs, writingStrategy, inputVersions) -> {
        if (!output.exists()) return true;
        long outputTime = output.lastModified();
        for (File input : inputs) {
            if (input.getName().equals("-")) continue;
            if (input.lastModified() > outputTime) return true;
        }
        return false;
    };

    RegenerationCheckStrategy VERSIONMARKER = new VersionMarkerRegenerationCheckStrategy();

    public class VersionMarkerRegenerationCheckStrategy implements RegenerationCheckStrategy {

        @Override
        public boolean needsRegeneration(@Nonnull File output, @Nonnull List<File> inputs,
                                         @Nonnull WritingStrategy writingStrategy, @Nonnull List<String> inputVersions) throws IOException {
            if (!output.exists()) {
                return true;
            }
            AIVersionMarker outputVersionMarker = writingStrategy.getRecordedVersionMarker(output);
            if (outputVersionMarker == null) {
                return true;
            }
            List<String> oldInputVersions = outputVersionMarker.getInputVersions();
            return !new HashSet<Object>(inputVersions).equals(new HashSet<Object>(oldInputVersions));
        }

    }

    Map<String, RegenerationCheckStrategy> STRATEGIES = Map.of(
            "a", ALWAYS,
            "n", IF_NOT_EXISTS,
            "o", IF_OLDER,
            "v", VERSIONMARKER
    );


}
