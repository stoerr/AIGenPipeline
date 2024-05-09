package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * AIInput is an interface that represents an input source for AI tasks.
 * It provides a method to read the input as a string.
 */
public interface AIInput {

    /**
     * Reads the input and returns it as a string.
     *
     * @return the input as a string
     * @throws IOException if an I/O error occurs
     */
    String read() throws IOException;

    /**
     * Creates an AIInput instance that reads from a file.
     *
     * @param file the file to read from
     * @return an AIInput instance
     */
    static AIInput of(File file) {
        return new AIFileInput(file);
    }

    /**
     * Creates an AIInput instance that reads from a segment of a segmented file.
     *
     * @param segmentedFile the segmented file to read from
     * @param segmentIndex  the index of the segment to read
     * @return an AIInput instance
     */
    static AIInput of(SegmentedFile segmentedFile, int segmentIndex) {
        return new AIFileSegmentInput(segmentedFile, segmentIndex);
    }

    /**
     * AIFileInput is an implementation of AIInput that reads from a file.
     */
    static class AIFileInput implements AIInput {

        private final File file;

        /**
         * Constructs an AIFileInput instance.
         *
         * @param file the file to read from
         */
        public AIFileInput(File file) {
            this.file = file;
        }

        /**
         * Reads the file and returns its content as a string.
         *
         * @return the file content as a string
         * @throws IOException if an I/O error occurs
         */
        public String read() throws IOException {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "AIFileInput [file=" + file + "]";
        }
    }

    /**
     * AIFileSegmentInput is an implementation of AIInput that reads from a segment of a segmented file.
     */
    static class AIFileSegmentInput implements AIInput {

        protected final SegmentedFile segmentedFile;
        protected final int segmentIndex;

        /**
         * Constructs an AIFileSegmentInput instance.
         *
         * @param segmentedFile the segmented file to read from
         * @param segmentIndex  the index of the segment to read
         */
        public AIFileSegmentInput(SegmentedFile segmentedFile, int segmentIndex) {
            this.segmentedFile = segmentedFile;
            this.segmentIndex = segmentIndex;
        }

        /**
         * Reads the segment from the segmented file and returns its content as a string.
         *
         * @return the segment content as a string
         * @throws IOException if an I/O error occurs
         */
        public String read() throws IOException {
            return segmentedFile.getSegment(segmentIndex);
        }

        @Override
        public String toString() {
            return "AIFileSegmentInput [segmentedFile=" + segmentedFile + ", segmentIndex=" + segmentIndex + "]";
        }
    }

}
