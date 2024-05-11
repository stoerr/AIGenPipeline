package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Ways to write a file and embed the version comment.
 */
public interface WritingStrategy {

    void write(@Nonnull AIInOut output, @Nonnull String content, @Nonnull String versionComment);

    /**
     * Version of current output file.
     */
    AIVersionMarker getRecordedVersionMarker(@Nonnull AIInOut output);

    /**
     * Writes the raw file without the cersion comment.
     */
    WritingStrategy WITHOUTVERSION = new WritingStrategy() {
        @Override
        public void write(@Nonnull AIInOut output, @Nonnull String content, @Nonnull String versionComment)  {
            output.write(content);
        }

        @Override
        public AIVersionMarker getRecordedVersionMarker(@Nonnull AIInOut output) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Cannot read version marker from file without version comment.");
        }


        @Override
        public String toString() {
            return "WritingStrategy.WITHOUTVERSION";
        }
    };

    /**
     * Writes the file with the version comment.
     */
    WritingStrategy WITHVERSION = new WritingStrategy() {
        @Override
        public void write(@Nonnull AIInOut output, @Nonnull String content, @Nonnull String versionComment)  {
            output.write(embedComment(output, content, versionComment));
        }

        @Override
        public AIVersionMarker getRecordedVersionMarker(@Nonnull AIInOut output) {
            String content;
            try {
                content = output.read();
            } catch (RuntimeException e) {
                return null;
            }
            if (content == null) {
                return null;
            }
            AIVersionMarker aiVersionMarker = AIVersionMarker.find(content);
            if (aiVersionMarker == null) {
                throw new IllegalStateException("Could not find version marker in " + output);
            }
            return aiVersionMarker;
        }

        protected String embedComment(@Nonnull AIInOut outputFile, String content, String comment) {
            String result;
            String extension = outputFile.getFile().getName().substring(outputFile.getFile().getName().lastIndexOf('.') + 1);
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

        @Override
        public String toString() {
            return "WritingStrategy.WITHVERSION";
        }
    };

}
