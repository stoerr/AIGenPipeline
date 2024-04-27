package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

/**
 * Ways to write a file and embed the version comment.
 */
public interface WritingStrategy {

    void write(@Nonnull File output, @Nonnull String content, @Nonnull String versionComment) throws IOException;

    /**
     * Version of current output file.
     */
    AIVersionMarker getRecordedVersionMarker(@Nonnull File output) throws IOException;

    /**
     * Writes the raw file without the cersion comment.
     */
    WritingStrategy WITHOUTVERSION = new WritingStrategy() {
        @Override
        public void write(@Nonnull File output, @Nonnull String content, @Nonnull String versionComment) throws IOException {
            Files.write(output.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public AIVersionMarker getRecordedVersionMarker(@Nonnull File output) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Cannot read version marker from file without version comment.");
        }
    };

    /**
     * Writes the file with the version comment.
     */
    WritingStrategy WITHVERSION = new WritingStrategy() {
        @Override
        public void write(@Nonnull File output, @Nonnull String content, @Nonnull String versionComment) throws IOException {
            Files.write(output.toPath(), embedComment(output, content, versionComment).getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public AIVersionMarker getRecordedVersionMarker(@Nonnull File output) throws IOException {
            if (!output.exists()) {
                return null;
            }
            String content = Files.readString(output.toPath(), StandardCharsets.UTF_8);
            if (content == null) {
                return null;
            }
            AIVersionMarker aiVersionMarker = AIVersionMarker.find(content);
            if (aiVersionMarker == null) {
                throw new IllegalStateException("Could not find version marker in " + output);
            }
            return aiVersionMarker;
        }

        protected String embedComment(@Nonnull File outputFile, String content, String comment) {
            String result;
            String extension = outputFile.getName().substring(outputFile.getName().lastIndexOf('.') + 1);
            switch (extension) {
                case "java":
                case "txt": // there is no real comment syntax for txt, but that might be obvious to a human reader
                    result = "// " + comment + "\n\n" + content;
                    break;
                case "html":
                case "htm":
                case "xml":
                case "jsp":
                    result = content + "\n\n<!-- " + comment + " -->\n";
                    break;
                case "css":
                case "js":
                case "json": // json is a problem, no comment syntax. Let's see whether this makes sense.
                    result = "/* " + comment + " */\n\n" + content;
                    break;
                case "md":
                    if (content.startsWith("---\n")) {
                        result = content.replaceFirst("---\n", "---\nversion: " + comment + "\n");
                    } else {
                        result = "---\nversion: " + comment + "\n---\n\n" + content;
                    }
                    break;
                case "sh":
                case "yaml":
                    result = "# " + comment + "\n" + content;
                    break;
                default:
                    result = "/* " + comment + " */\n\n" + content;
                    break;
            }
            if (!result.endsWith("\n")) {
                result += "\n";
            }
            return result;
        }

    };

    class WritePartStrategy implements WritingStrategy {
        private final String marker;

        public WritePartStrategy(String marker) {
            this.marker = marker;
        }

        /**
         * Find the first line of the file that contains the marker - that should also contain the version comment.
         */
        @Override
        public AIVersionMarker getRecordedVersionMarker(@Nonnull File output) throws IOException {
            try (Stream<String> lines = Files.lines(output.toPath())) {
                String markerLine = lines
                        .filter(line -> line.contains(marker))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not find marker " + marker + " in " + output));
                AIVersionMarker versionMarker = AIVersionMarker.find(markerLine);
                if (versionMarker == null) {
                    throw new IllegalStateException("Could not find version marker in " + output + " line " + markerLine);
                }
                return versionMarker;
            }
        }

        /**
         * Replaces the lines between the first line with marker and the second line with the marker with the content.
         * The first line should contain a version comment that is replaced by the new version comment.
         * It is an error if the marker is not exactly twice in the output file.
         *
         * @param output         the file to write to, has to exist
         * @param content        the content to write
         * @param versionComment the version comment to write
         * @throws IOException if the file cannot be read or written
         */
        @Override
        public void write(@Nonnull File output, @Nonnull String content, @Nonnull String versionComment) throws IOException {
            if (!output.exists()) {
                throw new IllegalArgumentException("File " + output + " does not exist. Required for WritePartStrategy.");
            }
            List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
            if (lines.stream().filter(line -> line.contains(marker)).count() != 2) {
                throw new IllegalArgumentException("Marker " + marker + " is not exactly twice in " + output);
            }
            int firstMarker = lines.indexOf(lines.stream().filter(line -> line.contains(marker)).findFirst().get());
            List<String> rest = lines.subList(firstMarker + 1, lines.size());
            int secondMarker = rest.indexOf(rest.stream().filter(line -> line.contains(marker)).findFirst().get()) + firstMarker + 1;
            if (content.contains(marker)) {
                throw new IllegalArgumentException("Content contains marker " + marker + ". That would lead to trouble next time.");
            }
            String firstMarkerLineNew = AIVersionMarker.replaceMarkerIn(lines.get(firstMarker), versionComment);
            if (AIVersionMarker.find(firstMarkerLineNew) == null) {
                throw new IllegalArgumentException("Bug: cannot find new version marker in new first marker line" + firstMarkerLineNew);
            }
            List<String> newLines = new ArrayList<>(lines.subList(0, firstMarker));
            newLines.add(firstMarkerLineNew);
            newLines.add(content);
            newLines.addAll(lines.subList(secondMarker, lines.size()));
            Files.write(output.toPath(), newLines, StandardCharsets.UTF_8);
        }

    }

}
