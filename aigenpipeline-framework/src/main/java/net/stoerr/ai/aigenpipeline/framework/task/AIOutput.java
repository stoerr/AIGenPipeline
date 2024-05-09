package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * AIOutput is an interface that represents an output destination for AI tasks.
 * It provides a method to write a string to the output.
 */
public interface AIOutput {

    /**
     * Writes a string to the output.
     *
     * @param content the string to write
     * @throws IOException if an I/O error occurs
     */
    void write(String content) throws IOException;

    /**
     * Creates an AIOutput instance that writes to a file.
     *
     * @param file the file to write to
     * @return an AIOutput instance
     */
    static AIOutput of(File file) {
        return new AIFileOutput(file);
    }

    /**
     * Creates an AIOutput instance that writes to a segment of a segmented file.
     *
     * @param segmentedFile the segmented file to write to
     * @param segmentIndex  the index of the segment to write to
     * @return an AIOutput instance
     */
    static AIOutput of(SegmentedFile segmentedFile, int segmentIndex) {
        return new AIFileSegmentOutput(segmentedFile, segmentIndex);
    }

    /**
     * AIFileOutput is an implementation of AIOutput that writes to a file.
     */
    static class AIFileOutput implements AIOutput {

        protected final File output;

        /**
         * Constructs an AIFileOutput instance.
         *
         * @param output the file to write to
         */
        public AIFileOutput(File output) {
            this.output = output;
        }

        /**
         * Writes a string to the file.
         *
         * @param content the string to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(String content) throws IOException {
            Files.write(output.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String toString() {
            return "AIFileOutput [output=" + output + "]";
        }
    }

    /**
     * AIFileSegmentOutput is an implementation of AIOutput that writes to a segment of a segmented file.
     */
    static class AIFileSegmentOutput implements AIOutput {

        protected final SegmentedFile segmentedFile;
        protected final int segmentIndex;

        /**
         * Constructs an AIFileSegmentOutput instance.
         *
         * @param segmentedFile the segmented file to write to
         * @param segmentIndex  the index of the segment to write to
         */
        public AIFileSegmentOutput(SegmentedFile segmentedFile, int segmentIndex) {
            this.segmentedFile = segmentedFile;
            this.segmentIndex = segmentIndex;
        }

        /**
         * Writes a string to the segment of the segmented file.
         *
         * @param content the string to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(String content) throws IOException {
            segmentedFile.writeSegment(segmentIndex, content);
        }

        @Override
        public String toString() {
            return "AIFileSegmentOutput [segmentedFile=" + segmentedFile + ", segmentIndex=" + segmentIndex + "]";
        }
    }

}
