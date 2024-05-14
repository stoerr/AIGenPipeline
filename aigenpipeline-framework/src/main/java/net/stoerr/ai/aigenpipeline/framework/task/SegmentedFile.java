package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * ALlows reading and writing individual segments of a file where the segments are separated by a regular expression.
 */
public class SegmentedFile {

    protected final File file;
    protected final List<Pattern> separatorPatterns;

    /**
     * Segment before first separator, content matching first separator, segment before second separator, content
     * matching second separator , and so forth, and then the rest of the file.
     **/
    protected List<String> segments;

    /**
     * Reads the file and checks that the separators can be found and that they don't match at other places.
     */
    public SegmentedFile(@Nonnull File file, @Nonnull String... separatorRegexes) throws IOException {
        this.file = file;
        this.separatorPatterns = Arrays.stream(separatorRegexes)
                .map(Pattern::compile)
                .collect(Collectors.toList());
        readAndParseFile();
    }

    /**
     * Reads the file and splits it according to the separators.
     */
    protected void readAndParseFile() throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        int startpos = 0;
        segments = new ArrayList<>();
        for (Pattern separator : separatorPatterns) {
            Matcher matcher = separator.matcher(content);
            if (!matcher.find(startpos)) {
                throw new IllegalArgumentException("Separator " + separator + " not found in " + file);
            }
            segments.add(content.substring(startpos, matcher.start()));
            segments.add(content.substring(matcher.start(), matcher.end()));
            startpos = matcher.end();
        }
        segments.add(content.substring(startpos));
        sanityCheck(content);
    }

    /**
     * Retrieves the segment - 0 is before first separator, 1 is between first and second separator, and so forth,
     * segments.size()-1 is the rest of the file.
     */
    public String getSegment(int i) {
        return segments.get(2 * i);
    }

    /**
     * Retrieves the segment - 0 is before first separator, 1 is between first and second separator, and so forth,
     * segments.size()-1 is the rest of the file.
     */
    public void writeSegment(int i, String newSegment) throws IOException {
        segments.set(2 * i, newSegment);
        Files.write(file.toPath(), joinSegments().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The basic file we write into.
     */
    public File getFile() {
        return file;
    }

    protected void sanityCheck(String content) {
        if (segments.size() != 2 * separatorPatterns.size() + 1) { // sanity check
            throw new IllegalStateException("Bug: parsed " + segments.size() + " segments, but expected "
                    + (2 * separatorPatterns.size() + 1));
        }
        // check that the separatorRegexes do not match any of the segments other than the separators.
        for (int i = 0; i < segments.size(); i += 2) {
            for (Pattern separator : separatorPatterns) {
                String segment = segments.get(i);
                Matcher matcher = separator.matcher(segment);
                if (matcher.find() && matcher.start() < segment.length() - 1) {
                    throw new IllegalStateException("Likely a usage error: separator " + separator + " matches segment " + i + " in " + file);
                }
            }
        }
        if (!content.equals(joinSegments())) {
            throw new IllegalStateException("Bug: content does not match segments in " + file);
        }
    }

    protected String joinSegments() {
        return String.join("", segments);
    }

    @Override
    public String toString() {
        return "SegmentedFile [file=" + file + ", separatorPatterns=" + separatorPatterns + "]";
    }

    public static String wholeLineRegex(String separator) {
        return ".*" + separator + ".*\n";
    }

    /**
     * Generates the patterns for storing prompt and generated data within one file.
     */
    public static String[] infilePrompting(String id) {
        return new String[]{
                wholeLineRegex("AIGenPromptStart\\(" + id + "\\)"),
                wholeLineRegex("AIGenCommand\\(" + id + "\\)"),
                wholeLineRegex("AIGenPromptEnd\\(" + id + "\\)"),
                wholeLineRegex("AIGenEnd\\(" + id + "\\)") + "|\\z"
        };
    }

    /**
     * Start pattern for {@link #infilePrompting(String)}, group "id" is the id.
     */
    public static final Pattern REGEX_AIGENPROMPTSTART = Pattern.compile("AIGenPromptStart\\((?<id>[^\\)]+)\\)");

}
