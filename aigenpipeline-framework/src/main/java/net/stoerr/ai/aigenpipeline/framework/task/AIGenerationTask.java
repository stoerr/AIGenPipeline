package net.stoerr.ai.aigenpipeline.framework.task;

import static java.util.Objects.requireNonNull;
import static net.stoerr.ai.aigenpipeline.framework.task.AIVersionMarker.shaHash;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;

/**
 * We support the generation of files using an AI, specifically ChatGPT. A generation task can have several input files.
 * Some of them can be prompt files with task descriptions, and some of them source files to be processed. The output
 * of each task is one text file. A complex task can have several steps leading to several intermediate files.
 * <p>
 * Since ChatGPT is not particularily fast not free and the generation results have to be manually checked, this is
 * heavily cached.
 * Into each output file we write the versions of all the input files from which it was generated into a comment.
 * When the tasks are run, we compare the
 * versions of all the input files with the versions recorded in the comment, and only regenerate the output file if
 * the versions have changed. An input file can have a version comment that explicitly states the version, or we take the
 * an abbreviated SHA256 hash of the input file as version. It is possible to explicitly state the versions in
 * version comments in the input files to avoid regenerating all files if minor details e.g. in a prompt file are
 * changed - only when the prompt file version comment is changed everything is regenerated.
 * <p>
 * A version comment can e.g. look like this:
 * <p>
 * // AIGenVersion(ourversion, inputfile1@version1, inputfile2@version2, ...)
 * <p>
 * where ourversion and version1 and version2 are hashes. ourversion is the hash of the original output of the AI.
 * The comment syntax (in this case //) is ignored - we just look for the AIGenVersion via regex.
 * <p>
 * Normally the intermediate and final results should be checked in with Git. That ensures manual checks when
 * they are regenerated, and minimizes regeneration.
 */
public class AIGenerationTask implements Cloneable {

    protected static final Logger LOG = Logger.getLogger(AIGenerationTask.class.getName());

    /**
     * A marker that can be inserted by the AI when something is wrong / unclear. We will make sure the user
     * sees that by aborting.
     */
    public static final String FIXME = "FIXME";

    /**
     * A pattern that matches the license header, which we want to remove to avoid clutter.
     */
    protected static final Pattern PATTERN_LICENCE =
            Pattern.compile("\\A<!--(?s).*?Copyright.*?Adobe.*?Licensed under.*?-->");


    protected List<File> inputFiles = new ArrayList<>();
    protected File outputFile;

    /**
     * The actual prompt created from prompt files and parameters.
     */
    protected String prompt;
    protected List<File> promptFiles = new ArrayList<>();
    protected Map<String, String> placeholdersAndValues = new LinkedHashMap<>();

    protected String systemMessage;
    protected File systemMessageFile;
    protected Integer maxTokens;
    protected RegenerationCheckStrategy regenerationCheckStrategy = RegenerationCheckStrategy.VERSIONMARKER;
    protected WritingStrategy writingStrategy = WritingStrategy.WITHVERSION;

    /**
     * Creates a deep copy of the task.
     */
    public AIGenerationTask copy() {
        try {
            AIGenerationTask copy = (AIGenerationTask) super.clone();
            copy.inputFiles = new ArrayList<>(inputFiles);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Bug - impossible.", e);
        }
    }

    public AIGenerationTask maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public AIGenerationTask addOptionalInputFile(@Nullable File file) {
        if (file != null && file.exists()) {
            inputFiles.add(file);
        } else {
            LOG.fine(() -> "Optional file not there: " + file);
        }
        return this;
    }

    public AIGenerationTask addInputFiles(List<File> files) {
        for (File file : files) {
            addInputFile(file);
        }
        return this;
    }

    public AIGenerationTask addInputFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file + " does not exist");
        }
        inputFiles.add(file);
        return this;
    }

    public AIGenerationTask setOutputFile(@Nonnull File file) {
        requireNonNull(file, "File must not be null");
        outputFile = file;
        return this;
    }

    /**
     * Sets the strategy to check whether the generation has to be run. Default is {@link RegenerationCheckStrategy#VERSIONMARKER}.
     */
    public AIGenerationTask setRegenerationCheckStrategy(RegenerationCheckStrategy strategy) {
        this.regenerationCheckStrategy = strategy;
        return this;
    }

    /**
     * Sets the strategy to deal with writing the output file. Default is {@link WritingStrategy#WITHVERSION}.
     */
    public AIGenerationTask setWritingStrategy(WritingStrategy strategy) {
        this.writingStrategy = strategy;
        return this;
    }

    public boolean hasToBeRun() throws IOException {
        List<File> allInputs = getAllInputFiles();
        List<String> additionalMarkers = getAdditionalMarkers();
        List<String> inputVersions = AIVersionMarker.calculateInputMarkers(allInputs, additionalMarkers);
        return regenerationCheckStrategy.needsRegeneration(outputFile, allInputs, writingStrategy, inputVersions);
    }

    protected List<String> getAdditionalMarkers() {
        List<String> additionalMarkers = new ArrayList<>();
        if (!placeholdersAndValues.isEmpty()) {
            additionalMarkers.add("parms-" + shaHash(placeholdersAndValues.toString()));
        }
        return additionalMarkers;
    }

    protected List<File> getAllInputFiles() {
        List<File> allInputs = new ArrayList<>();
        if (systemMessageFile != null) {
            allInputs.add(systemMessageFile);
        }
        promptFiles.stream()
                .filter(f -> !f.getAbsolutePath().equals(outputFile.getAbsolutePath()))
                .forEach(allInputs::add);
        inputFiles.stream()
                // don't introduce circular dependencies when updating an existing output file:
                .filter(f -> !f.getAbsolutePath().equals(outputFile.getAbsolutePath()))
                .forEach(allInputs::add);
        return allInputs;
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given: placeholdersAndValues contain alternatingly placeholder names and values for them.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull File promptFile, String... placeholdersAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < placeholdersAndValues.length; i += 2) {
            map.put(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        return addPrompt(promptFile, map);
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull File promptFile, Map<String, String> placeholdersAndValues) {
        String fileContent = getFileContent(promptFile);
        if (fileContent == null) {
            throw new IllegalArgumentException("Could not read prompt file " + promptFile);
        }
        String newPrompt = unclutter(fileContent);
        requireNonNull(newPrompt, "Could not read prompt file " + promptFile);
        for (Map.Entry<String, String> entry : placeholdersAndValues.entrySet()) {
            newPrompt = newPrompt.replace(entry.getKey(), entry.getValue());
        }
        if (this.prompt == null) {
            this.prompt = newPrompt;
        } else {
            this.prompt += "\n\n" + newPrompt;
        }
        this.promptFiles.add(promptFile);
        this.placeholdersAndValues.putAll(placeholdersAndValues);
        return this;
    }

    @Nullable
    protected String getFileContent(@Nonnull File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading file " + file, e);
        }
    }

    /* Remove some clutter that is not relevant and might even confuse the AI */
    protected static String unclutter(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = PATTERN_LICENCE.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceFirst("");
        }
        matcher = AIVersionMarker.VERSION_MARKER_PATTERN.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceFirst("");
        }
        return content;
    }

    public AIGenerationTask setSystemMessage(@Nonnull File systemMessageFile) {
        String fileContent = getFileContent(systemMessageFile);
        if (fileContent == null) {
            throw new IllegalArgumentException("Could not read system message file " + systemMessageFile);
        }
        String newSystemMessage = unclutter(fileContent);
        requireNonNull(newSystemMessage, "Could not read system message file " + systemMessageFile);
        this.systemMessage = newSystemMessage;
        this.systemMessageFile = systemMessageFile;
        return this;
    }

    protected String relativePath(@Nullable File file, @Nonnull File rootDirectory) {
        if (file == null) {
            return null;
        }
        requireNonNull(rootDirectory, "Root directory must not be null");
        return rootDirectory.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString();
    }

    /**
     * Execute the task if necessary. If the output file is already there and up to date, nothing is done.
     */
    public AIGenerationTask execute(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) throws IOException {
        String outputRelPath = relativePath(this.outputFile, rootDirectory);
        if (!hasToBeRun()) {
            LOG.info(() -> "Task does not have to be run for: " + outputRelPath);
            return this;
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory, outputRelPath);
        String result = chat.execute();
        LOG.fine(() -> "Result for task execution for: " + outputRelPath + "\n" + result);
        String outputVersion = shaHash(result);

        List<String> allInputMarkers = AIVersionMarker.calculateInputMarkers(getAllInputFiles(), getAdditionalMarkers());
        String versionComment = new AIVersionMarker(outputVersion, allInputMarkers).toString();

        writingStrategy.write(outputFile, result, versionComment);

        // We check that after writing since that likely makes it easier to check.
        if (result.contains(FIXME)) {
            throw new IllegalStateException("AI returned FIXME for " + outputRelPath + " :\n" + result);
        }
        return this;
    }

    /**
     * For debugging purposes: returns the JSON that would be sent to the AI.
     */
    public String toJson(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) {
        String outputRelPath = relativePath(this.outputFile, rootDirectory);
        return makeChatBuilder(chatBuilderFactory, rootDirectory, outputRelPath).toJson();
    }

    @Nonnull
    protected AIChatBuilder makeChatBuilder(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory, String outputRelPath) {
        requireNonNull(outputFile, "Output file not writeable: " + outputFile);
        if (null != outputFile.getParentFile() && !outputFile.getParentFile().isDirectory()) {
            outputFile.getParentFile().mkdirs();
        }
        if (outputFile.exists() && !outputFile.canWrite()) {
            throw new IllegalArgumentException("No writeable output file given! " + outputFile);
        }
        if ((prompt == null || prompt.isBlank()) && (systemMessage == null || systemMessage.isBlank()) && systemMessageFile == null) {
            throw new IllegalArgumentException("No prompt given!");
        }
        AIChatBuilder chat = chatBuilderFactory.get();
        if (maxTokens != null) {
            chat.maxTokens(maxTokens);
        }
        if (systemMessage != null) {
            chat.systemMsg(systemMessage);
        } else {
            try (InputStream defaultprompt = AIGenerationTask.class.getResourceAsStream("/defaultsystemprompt.txt")) {
                String defaultSysPrompt = new String(requireNonNull(defaultprompt).readAllBytes(), StandardCharsets.UTF_8);
                chat.systemMsg(defaultSysPrompt);
            } catch (IOException e) {
                throw new IllegalStateException("Error reading default system message", e);
            }
        }
        inputFiles.forEach(file -> {
            // "Put it into the AI's mouth" pattern https://www.stoerr.net/blog/aimouth
            String path = relativePath(file, rootDirectory);
            String usermsg = !file.getAbsolutePath().equals(outputFile.getAbsolutePath()) ?
                    "Retrieve the content of the input file '" + path + "'" :
                    "Retrieve the current content of the output file '" + path +
                            "'. Later you will take this file as basis for the output, check it and possibly modify it, " +
                            "but minimize changes.";
            chat.userMsg(usermsg);
            String fileContent = getFileContent(file);
            if (fileContent == null) {
                throw new IllegalArgumentException("Could not read input file " + file);
            }
            chat.assistantMsg(unclutter(fileContent));
        });
        chat.userMsg(prompt);
        LOG.fine(() -> "Executing chat for: " + outputRelPath + "\n" + chat.toJson());
        return chat;
    }

    /**
     * Ask a question about the previous task execution. We assume it was previously run ({@link #hasToBeRun()} == false),
     * add the result of the previous execution to the chat, and ask the AI the given question about it.
     * This can be used e.g. to see why the AI did something, or in the process of improving the prompt, etc.
     *
     * @return the answer of the AI - not written to a file!
     */
    public String explain(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory, @Nonnull String question) throws IOException {
        String outputRelPath = relativePath(this.outputFile, rootDirectory);
        if (hasToBeRun()) { // that's not strictly necessary, but if not that's a likely mistake
            throw new IllegalStateException("Task has to be already run for: " + outputRelPath);
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory, outputRelPath);
        String outputFileContent = getFileContent(outputFile);
        if (outputFileContent == null) {
            throw new IllegalStateException("Usage error - no previous call? Could not read output file " + outputFile);
        }
        String previousOutput = unclutter(outputFileContent);
        requireNonNull(previousOutput, "Could not read any content from file " + outputFile);
        chat.assistantMsg(previousOutput);
        chat.userMsg(question);
        String result = chat.execute();
        LOG.info(() -> "Explanation result for " + outputRelPath + " with question " + question + " is:\n" + result);
        if (result.contains(FIXME)) {
            throw new IllegalStateException("AI returned FIXME for " + outputRelPath + " :\n" + result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "AIGenerationTask{" + "inputFiles=" + inputFiles +
                ", outputFile=" + outputFile +
                ", systemMessageFile=" + systemMessageFile +
                ", systemMessage='" + systemMessage + '\'' +
                ", promptFiles=" + promptFiles +
                ", placeholdersAndValues=" + placeholdersAndValues +
                ", prompt='" + prompt + '\'' +
                '}';
    }
}
