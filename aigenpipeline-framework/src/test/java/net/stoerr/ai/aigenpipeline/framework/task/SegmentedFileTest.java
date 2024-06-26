package net.stoerr.ai.aigenpipeline.framework.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SegmentedFileTest {

    private File testFile;

    @Before
    public void setUp() throws IOException {
        Path tempDir = Paths.get("target");
        testFile = Files.createTempFile(tempDir, "segmentedFileTest", ".txt").toFile();
        testFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        testFile.delete();
    }

    @Test
    public void readsAndSplitsFileCorrectly() throws IOException {
        Files.writeString(testFile.toPath(), "Segment1\n===\nSegment2\n===\nSegment3");
        String[] separators = new String[]{"===\n", "===\n"};
        SegmentedFile segmentedFile = new SegmentedFile(testFile, separators);

        assertEquals("Segment1\n", segmentedFile.getSegment(0));
        assertEquals("Segment2\n", segmentedFile.getSegment(1));
        assertEquals("Segment3", segmentedFile.getSegment(2));
    }

    @Test
    public void writesSegmentCorrectly() throws IOException {
        Files.writeString(testFile.toPath(), "Segment1\n==s1=\nSegment2\n==s2=\nSegment3");
        String[] separators = new String[]{"==s1=\n", "==s2=\n"};
        SegmentedFile segmentedFile = new SegmentedFile(testFile, separators);

        segmentedFile.writeSegment(1, "NewSegment2\n");

        assertEquals("Segment1\n", segmentedFile.getSegment(0));
        assertEquals("NewSegment2\n", segmentedFile.getSegment(1));
        assertEquals("Segment3", segmentedFile.getSegment(2));
    }

    @Test
    public void throwsExceptionWhenSeparatorNotFound() throws IOException {
        Files.writeString(testFile.toPath(), "Segment1\n===\nSegment2\n===\nSegment3");
        assertThrows(IllegalArgumentException.class, () -> new SegmentedFile(testFile, new String[]{"---"}));
    }

    @Test
    public void throwsExceptionWhenSeparatorMatchesSegment() throws IOException {
        Files.writeString(testFile.toPath(), "Segment1\n===\nSegment2===\n===\nSegment3");
        String[] separators = new String[]{"==="};
        assertThrows(IllegalStateException.class, () -> new SegmentedFile(testFile, separators));
    }

    @Test
    public void multilineSeparator() throws IOException {
        Files.writeString(testFile.toPath(), "Segment1\n/*separ\nated*/\nSegment2");
        // pattern to match a line containing separ to a line containing ated
        String[] separators = new String[]{"(?m)^.*separ((?s).*?)ated.*$\n"};
        SegmentedFile segmentedFile = new SegmentedFile(testFile, separators);
        assertEquals("Segment1\n", segmentedFile.getSegment(0));
        assertEquals("Segment2", segmentedFile.getSegment(1));
    }

    @Test
    public void wholeLineRegex() {
        assertEquals(".*===.*\n", SegmentedFile.wholeLineRegex("==="));
    }

    @Test
    public void testInfilePrompt() throws IOException {
        File withprompts = new File("src/test/resources/infileprompt-test/withprompts.md");
        assertTrue("Doesn't exist: " + withprompts.getAbsolutePath(), withprompts.exists());
        String[] separators = SegmentedFile.infilePrompting("tablefromdata");
        SegmentedFile segmentedFile = new SegmentedFile(withprompts, separators);

        assertEquals("Start of the file\n", segmentedFile.getSegment(0));
        assertEquals("Make a markdown table from the data, with columns \"Name\" and \"Profession\".\n", segmentedFile.getSegment(1));
        assertEquals("data.txt\n", segmentedFile.getSegment(2));
        assertEquals("Here is the generated content\n", segmentedFile.getSegment(3));
        assertEquals("End of the file\n", segmentedFile.getSegment(4));
    }

    @Test
    public void testInfilePromptNoEndMarker() throws IOException {
        File noendmarker = new File("src/test/resources/infileprompt-test/noendmarker.md");
        assertTrue("Doesn't exist: " + noendmarker.getAbsolutePath(), noendmarker.exists());
        String[] separators = SegmentedFile.infilePrompting("tablefromdata");
        SegmentedFile segmentedFile = new SegmentedFile(noendmarker, separators);

        assertEquals("Start of the file\n", segmentedFile.getSegment(0));
        assertEquals("Make a markdown table from the data, with columns \"Name\" and \"Profession\".\n", segmentedFile.getSegment(1));
        assertEquals("data.txt\n", segmentedFile.getSegment(2));
        assertEquals("Here is the generated content\n", segmentedFile.getSegment(3));
    }

}
