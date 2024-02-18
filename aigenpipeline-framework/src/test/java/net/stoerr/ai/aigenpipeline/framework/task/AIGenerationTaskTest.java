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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AIGenerationTaskTest {

    Path inputDir = new File("src/test/resources/aigenpipeline-test").toPath();
    Path expectsDir = inputDir.resolve("expected");
    Path tempDir;

    @Before
    public void setUp() throws Exception {
        Path targetDir = Paths.get("target");
        Assert.assertTrue(Files.exists(targetDir));
        tempDir = targetDir.resolve("aigenpipeline-test-output");
        // Files.createTempDirectory(targetDir, "aigenpipeline-test");
        deleteDirectory(tempDir);
        Files.createDirectory(tempDir);
        Assert.assertTrue(Files.exists(inputDir));
        Assert.assertTrue(Files.exists(expectsDir));
    }

    protected void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            List<Path> children = Files.walk(dir, 1).collect(Collectors.toList());
            Collections.reverse(children);
            for (Path child : children) {
                Files.delete(child);
            }
        }
    }

    @Test
    public void testCompleteAIGenerationProcess() throws Exception {

        MockAIChatBuilder chatBuilder = new MockAIChatBuilder();
        AIGenerationTask task = new AIGenerationTask();

        task.setPrompt(inputDir.resolve("prompt.txt").toFile());
        task.addInputFile(inputDir.resolve("input.txt").toFile());
        Path outFile = tempDir.resolve("output.txt");
        task.setOutputFile(outFile.toFile());

        Assert.assertTrue(task.hasToBeRun());
        task.execute(MockAIChatBuilder::new, new File("."));

        checkOutputExistsAndIsAsExpected(outFile);

        Assert.assertFalse(task.hasToBeRun());
    }

    private void checkOutputExistsAndIsAsExpected(Path outFile) throws IOException {
        assertTrue(Files.exists(outFile));
        String outputContent = Files.readString(outFile);
        assertEquals(Files.readString(expectsDir.resolve(outFile.getFileName())), outputContent);
    }
}
