package net.stoerr.ai.aigenpipeline.framework.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AIGenerationTaskTest {

    Path inputDir = new File("src/test/resources/aigenpipeline-test").toPath();
    Path expectsDir = inputDir.resolve("expected");
    static Path tempDir;

    @BeforeClass
    public static void setUpClass() throws IOException {
        Path targetDir = Paths.get("target");
        Assert.assertTrue(Files.exists(targetDir));
        tempDir = targetDir.resolve("aigenpipeline-test-output");
        // Files.createTempDirectory(targetDir, "aigenpipeline-test");
        deleteDirectory(tempDir);
        Files.createDirectory(tempDir);
    }

    @Before
    public void setUp() {
        Assert.assertTrue(Files.exists(inputDir));
        Assert.assertTrue(Files.exists(expectsDir));
    }

    protected static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir, 1)) {
                List<Path> children = walk.collect(Collectors.toList());
                Collections.reverse(children);
                for (Path child : children) {
                    Files.delete(child);
                }
            }
        }
    }

    protected void checkOutputExistsAndIsAsExpected(Path outFile) throws IOException {
        assertTrue(Files.exists(outFile));
        String outputContent = Files.readString(outFile);
        Path expectedFile = expectsDir.resolve(outFile.getFileName());
        assertEquals("Files are different: " + outFile + " and " + expectedFile,
                Files.readString(expectedFile), outputContent);
    }

    @Test
    public void testCompleteAIGenerationProcess() throws Exception {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(AIInOut.of(inputDir.resolve("prompt.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("input.txt").toFile()));
        Path outFile = tempDir.resolve("output.txt");
        task.setSystemMessage(AIInOut.of(inputDir.resolve("sysmsg.txt").toFile()));
        task.setOutput(AIInOut.of(outFile.toFile()));
        task.maxTokens(1000);

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));

        checkOutputExistsAndIsAsExpected(outFile);

        Assert.assertFalse(task.hasToBeRun());

        String result = task.explain(MockAIChatBuilder::new, new File("."), "Why oh why oh why?");
        assertEquals(Files.readString(expectsDir.resolve("explanation.txt")).trim(), result.trim());
    }

    @Test
    public void testVersionExtraction() throws Exception {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(AIInOut.of(inputDir.resolve("promptWithVersion.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("input.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("inputWithVersion.txt").toFile()));
        Path outFile = tempDir.resolve("outputWithVersion.txt");
        task.setOutput(AIInOut.of(outFile.toFile()));

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));

        checkOutputExistsAndIsAsExpected(outFile);

        Assert.assertFalse(task.hasToBeRun());
    }

    /**
     * Checks whether the deep copy works.
     */
    @Test
    public void testCopy() throws IOException {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(AIInOut.of(inputDir.resolve("prompt.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("input.txt").toFile()));
        Path outFile = tempDir.resolve("output.txt");
        task.setSystemMessage(AIInOut.of(inputDir.resolve("sysmsg.txt").toFile()));
        task.setOutput(AIInOut.of(outFile.toFile()));
        task.maxTokens(1000);

        AIGenerationTask copy = task.copy();
        assertEquals(task.toString(), copy.toString());
    }

    @Test
    public void testReplacePart() throws IOException {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(AIInOut.of(inputDir.resolve("prompt.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("input.txt").toFile()));
        task.addInput(AIInOut.of(inputDir.resolve("inputWithVersion.txt").toFile()));
        Path outFile = tempDir.resolve("outputWithReplacement.txt");
        Files.copy(inputDir.resolve("outputWithReplacement.txt"), outFile);
        SegmentedFile segmentedFile = new SegmentedFile(outFile.toFile(), new String[]{"thereplacedpart", "thereplacedpart"});
        task.setOutput(AIInOut.of(segmentedFile, 1));

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));
        checkOutputExistsAndIsAsExpected(outFile);
    }

}
