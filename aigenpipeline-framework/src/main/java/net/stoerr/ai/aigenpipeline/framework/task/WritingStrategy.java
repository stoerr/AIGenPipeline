package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

}
