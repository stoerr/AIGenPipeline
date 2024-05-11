package net.stoerr.ai.aigenpipeline.framework.task;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * AIInOut is an interface that represents an input or output for AI tasks.
 * We join input and output since in some cases we need to read the file, too.
 */
public interface AIInOut {

    /**
     * Reads the input and returns it as a string.
     *
     * @return the input as a string
     * @throws IllegalStateException if the input cannot be read
     */
    String read() throws IllegalStateException;

    /**
     * Writes a string to the output.
     *
     * @param content the string to write
     */
    void write(String content)  ;


    /**
     * The underlying file.
     */
    File getFile();

    default boolean sameFile(AIInOut other) {
        return getFile().getAbsolutePath().equals(other.getFile().getAbsolutePath());
    }

    default boolean exists() {
        return null != getFile() && getFile().exists();
    }

    /**
     * Creates an AIInOut instance that reads from a file.
     *
     * @param file the file to read from
     * @return an AIInOut instance
     */
    @Nullable
    static AIInOut of(@Nullable File file) {
        return file != null ? new AIFileInOut(requireNonNull(file)) : null;
    }

    /**
     * Creates an AIInOut instance that reads from a segment of a segmented file.
     *
     * @param segmentedFile the segmented file to read from
     * @param segmentIndex  the index of the segment to read
     * @return an AIInOut instance
     */
    @Nonnull
    static AIInOut of(@Nonnull SegmentedFile segmentedFile, int segmentIndex) {
        return new AIFileSegmentInOut(requireNonNull(segmentedFile), segmentIndex);
    }

    /**
     * AIFileInOut is an implementation of AIInOut that reads from a file.
     */
    static class AIFileInOut implements AIInOut {

        private final File file;

        /**
         * Constructs an AIFileInOut instance.
         *
         * @param file the file to read from
         */
        public AIFileInOut(File file) {
            this.file = file;
        }

        /**
         * Reads the file and returns its content as a string.
         *
         * @return the file content as a string
         */
        public String read() throws IllegalStateException {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + file, e);
            }
        }

        /**
         * Writes a string to the file.
         *
         * @param content the string to write
         */
        @Override
        public void write(String content)   {
            try {
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Could not write " + file, e);
            }
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return "AIFileInOut [file=" + file + "]";
        }
    }

    /**
     * AIFileSegmentInOut is an implementation of AIInOut that reads from a segment of a segmented file.
     */
    static class AIFileSegmentInOut implements AIInOut {

        protected final SegmentedFile segmentedFile;
        protected final int segmentIndex;

        /**
         * Constructs an AIFileSegmentInOut instance.
         *
         * @param segmentedFile the segmented file to read from
         * @param segmentIndex  the index of the segment to read
         */
        public AIFileSegmentInOut(SegmentedFile segmentedFile, int segmentIndex) {
            this.segmentedFile = segmentedFile;
            this.segmentIndex = segmentIndex;
        }

        /**
         * Reads the segment from the segmented file and returns its content as a string.
         *
         * @return the segment content as a string
         */
        public String read()   {
            return segmentedFile.getSegment(segmentIndex);
        }

        /**
         * Writes a string to the segment of the segmented file.
         *
         * @param content the string to write
         */
        @Override
        public void write(String content)   {
            try {
                segmentedFile.writeSegment(segmentIndex, content);
            } catch (IOException e) {
                throw new IllegalStateException("Could not write segment " + segmentIndex + " of " + segmentedFile, e);
            }
        }

        @Override
        public File getFile() {
            return segmentedFile.getFile();
        }

        @Override
        public String toString() {
            return "AIFileSegmentInOut [segmentedFile=" + segmentedFile + ", segmentIndex=" + segmentIndex + "]";
        }
    }

}
