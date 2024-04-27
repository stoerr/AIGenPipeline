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
        assertEquals(Files.readString(expectsDir.resolve(outFile.getFileName())), outputContent);
    }

    @Test
    public void testCompleteAIGenerationProcess() throws Exception {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(inputDir.resolve("prompt.txt").toFile());
        task.addInputFile(inputDir.resolve("input.txt").toFile());
        Path outFile = tempDir.resolve("output.txt");
        task.setSystemMessage(inputDir.resolve("sysmsg.txt").toFile());
        task.setOutputFile(outFile.toFile());
        task.maxTokens(1000);

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));

        checkOutputExistsAndIsAsExpected(outFile);

        Assert.assertFalse(task.hasToBeRun());

        String result = task.explain(MockAIChatBuilder::new, new File("."), "Why oh why oh why?");
        assertEquals(result.trim(), Files.readString(expectsDir.resolve("explanation.txt")).trim());
    }

    @Test
    public void testVersionExtraction() throws Exception {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(inputDir.resolve("promptWithVersion.txt").toFile());
        task.addInputFile(inputDir.resolve("input.txt").toFile());
        task.addInputFile(inputDir.resolve("inputWithVersion.txt").toFile());
        Path outFile = tempDir.resolve("outputWithVersion.txt");
        task.setOutputFile(outFile.toFile());

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));

        checkOutputExistsAndIsAsExpected(outFile);

        Assert.assertFalse(task.hasToBeRun());
    }

    /** Checks whether the deep copy works. */
    @Test
    public void testCopy() {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(inputDir.resolve("prompt.txt").toFile());
        task.addInputFile(inputDir.resolve("input.txt").toFile());
        Path outFile = tempDir.resolve("output.txt");
        task.setSystemMessage(inputDir.resolve("sysmsg.txt").toFile());
        task.setOutputFile(outFile.toFile());
        task.maxTokens(1000);

        AIGenerationTask copy = task.copy();
        assertEquals(task.toString(), copy.toString());
    }

    @Test
    public void testReplacePart() throws IOException {
        AIGenerationTask task = new AIGenerationTask();

        task.addPrompt(inputDir.resolve("prompt.txt").toFile());
        task.addInputFile(inputDir.resolve("input.txt").toFile());
        task.addInputFile(inputDir.resolve("inputWithVersion.txt").toFile());
        Path outFile = tempDir.resolve("outputWithReplacement.txt");
        Files.copy(inputDir.resolve("outputWithReplacement.txt"), outFile);
        task.setOutputFile(outFile.toFile());
        task.setWritingStrategy(new WritingStrategy.WritePartStrategy("thereplacedpart"));


        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));
        checkOutputExistsAndIsAsExpected(outFile);
    }

}
