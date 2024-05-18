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
    public static final String FIXME = "FIXME(GenAIPipeline)";

    /**
     * A pattern that matches the license header, which we want to remove to avoid clutter.
     */
    protected static final Pattern PATTERN_LICENCE =
            Pattern.compile("\\A<!--(?s).*?Copyright.*?Adobe.*?Licensed under.*?-->");

    /**
     * A pattern matching infile prompts like this:
     * <pre>
     * <%-- AIGenPromptStart(tablefromdatacopied)
     * Make a markdown table from the data, with columns "Name" and "Profession".
     * AIGenCommand(tablefromdatacopied)
     * -f -m copy tablefromdata.md
     * AIGenPromptEnd(tablefromdatacopied) --%>
     * </pre>
     * This matches a line containing AIGenPromptStart with an id until the corresponding AIGenPromptEnd.
     */
    protected static final Pattern PATTERN_INFILEPROMPT = Pattern.compile(
            ".*AIGenPromptStart\\(([^)]*)\\)((?s).*?)AIGenPromptEnd\\(\\1\\).*\n?");

    protected List<AIInOut> inputFiles = new ArrayList<>();
    protected AIInOut output;

    /**
     * The actual prompt created from prompt files and parameters.
     */
    protected String prompt;
    protected List<AIInOut> promptInputs = new ArrayList<>();
    protected Map<String, String> placeholdersAndValues = new LinkedHashMap<>();

    protected String systemMessage;
    protected AIInOut systemMessageInput;
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

    public AIGenerationTask addOptionalInput(@Nullable AIInOut input) {
        if (input != null && input.exists()) {
            inputFiles.add(input);
        } else {
            LOG.fine(() -> "Optional file not there: " + input);
        }
        return this;
    }

    public AIGenerationTask addOptionalInputFile(@Nullable File file) {
        return addOptionalInput(AIInOut.of(file));
    }

    public AIGenerationTask addInputFiles(List<File> files) {
        for (File file : files) {
            addInputFile(file);
        }
        return this;
    }

    public AIGenerationTask addInputs(List<AIInOut> inputs) {
        for (AIInOut input : inputs) {
            addInput(input);
        }
        return this;
    }

    public AIGenerationTask addInput(AIInOut input) {
        if (!input.exists()) {
            throw new IllegalArgumentException("File " + input + " does not exist");
        }
        inputFiles.add(input);
        return this;
    }

    public AIGenerationTask addInputFile(File file) {
        return addInput(AIInOut.of(file));
    }

    public AIGenerationTask setOutput(@Nonnull AIInOut output) {
        requireNonNull(output, "Ouput must not be null");
        this.output = output;
        return this;
    }

    public AIGenerationTask setOutputFile(@Nonnull File file) {
        return setOutput(AIInOut.of(file));
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

    public boolean hasToBeRun() {
        List<AIInOut> allInputs = getAllInputs();
        List<String> additionalMarkers = getAdditionalMarkers();
        List<String> inputVersions = AIVersionMarker.calculateInputMarkers(allInputs, additionalMarkers);
        return regenerationCheckStrategy.needsRegeneration(output, allInputs, writingStrategy, inputVersions);
    }

    protected List<String> getAdditionalMarkers() {
        List<String> additionalMarkers = new ArrayList<>();
        if (!placeholdersAndValues.isEmpty()) {
            additionalMarkers.add("parms-" + shaHash(placeholdersAndValues.toString()));
        }
        return additionalMarkers;
    }

    protected List<AIInOut> getAllInputs() {
        List<AIInOut> allInputs = new ArrayList<>();
        if (systemMessageInput != null) {
            allInputs.add(systemMessageInput);
        }
        allInputs.addAll(promptInputs);
        inputFiles.stream()
                // don't introduce circular dependencies when updating an existing output file:
                .filter(f -> !f.sameFile(output))
                .forEach(allInputs::add);
        return allInputs;
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given: placeholdersAndValues contain alternatingly placeholder names and values for them.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull AIInOut promptInput, String... placeholdersAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < placeholdersAndValues.length; i += 2) {
            map.put(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        return addPrompt(promptInput, map);
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given: placeholdersAndValues contain alternatingly placeholder names and values for them.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull File promptInput, String... placeholdersAndValues) {
        return addPrompt(AIInOut.of(promptInput), placeholdersAndValues);
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull AIInOut promptFile, Map<String, String> placeholdersAndValues) {
        String fileContent = promptFile.read();
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
        this.promptInputs.add(promptFile);
        this.placeholdersAndValues.putAll(placeholdersAndValues);
        return this;
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given.
     *
     * @return this
     */
    public AIGenerationTask addPrompt(@Nonnull File promptFile, Map<String, String> placeholdersAndValues) {
        return addPrompt(AIInOut.of(promptFile), placeholdersAndValues);
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

    /* Remove some clutter that is not relevant and might even confuse the AI. */
    public static String unclutter(String content) {
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
        matcher = PATTERN_INFILEPROMPT.matcher(content);
        if (matcher.find()) {
            content = matcher.replaceFirst("");
        }
        return content;
    }

    public AIGenerationTask setSystemMessage(@Nonnull AIInOut systemMessageFile) {
        String fileContent = systemMessageFile.read();
        if (fileContent == null) {
            throw new IllegalArgumentException("Could not read system message file " + systemMessageFile);
        }
        String newSystemMessage = unclutter(fileContent);
        requireNonNull(newSystemMessage, "Could not read system message file " + systemMessageFile);
        this.systemMessage = newSystemMessage;
        this.systemMessageInput = systemMessageFile;
        return this;
    }

    public AIGenerationTask setSystemMessage(@Nonnull File systemMessageFile) {
        return setSystemMessage(AIInOut.of(systemMessageFile));
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
    public AIGenerationTask execute(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) {
        if (!hasToBeRun()) {
            LOG.info(() -> "Task does not have to be run for: " + output);
            return this;
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory);
        String result = chat.execute();
        LOG.fine(() -> "Result for task execution for: " + output + "\n" + result);
        String outputVersion = shaHash(result);

        List<String> allInputMarkers = AIVersionMarker.calculateInputMarkers(getAllInputs(), getAdditionalMarkers());
        String versionComment = new AIVersionMarker(outputVersion, allInputMarkers).toString();

        writingStrategy.write(output, result, versionComment);

        // We check that after writing since that likely makes it easier to check.
        if (result.contains(FIXME)) {
            throw new IllegalStateException("AI returned FIXME for " + output + " :\n" + result);
        }
        return this;
    }

    /**
     * For debugging purposes: returns the JSON that would be sent to the AI.
     */
    public String toJson(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) {
        return makeChatBuilder(chatBuilderFactory, rootDirectory).toJson();
    }

    @Nonnull
    protected AIChatBuilder makeChatBuilder(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) {
        requireNonNull(output, "Output file not writeable: " + output);
        if ((prompt == null || prompt.isBlank()) && (systemMessage == null || systemMessage.isBlank()) && systemMessageInput == null) {
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
                throw new IllegalStateException("Bug: Error reading default system message", e);
            }
        }
        for (AIInOut file : inputFiles) {
            // "Put it into the AI's mouth" pattern https://www.stoerr.net/blog/aimouth
            String path = relativePath(file.getFile(), rootDirectory);
            String usermsg = !file.sameFile(output) ? // XXX WRONG
                    "Retrieve the content of the input file '" + path + "'" :
                    "Retrieve the current content of the output file '" + path +
                            "'. Later you will take this file as basis for the output, check it and possibly modify it, " +
                            "but minimize changes.";
            chat.userMsg(usermsg);
            String fileContent = file.read();
            if (fileContent == null) {
                throw new IllegalArgumentException("Could not read input file " + file);
            }
            chat.assistantMsg(unclutter(fileContent));
        }
        chat.userMsg(prompt);
        LOG.fine(() -> "Executing chat for: " + output + "\n" + chat.toJson());
        return chat;
    }

    /**
     * Ask a question about the previous task execution. We assume it was previously run ({@link #hasToBeRun()} == false),
     * add the result of the previous execution to the chat, and ask the AI the given question about it.
     * This can be used e.g. to see why the AI did something, or in the process of improving the prompt, etc.
     *
     * @return the answer of the AI - not written to a file!
     */
    public String explain(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory, @Nonnull String question) {
        if (hasToBeRun()) { // that's not strictly necessary, but if not that's a likely mistake
            System.err.println("Warning: explain results might be invalid since task would need to be run for: " + output);
            // throw new IllegalStateException("Task has to be already run for: " + output);
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory);
        String outputFileContent = output.read();
        if (outputFileContent == null) {
            throw new IllegalStateException("Usage error - no previous call? Could not read output file " + output);
        }
        String previousOutput = unclutter(outputFileContent);
        requireNonNull(previousOutput, "Could not read any content from file " + output);
        chat.assistantMsg(previousOutput);
        chat.userMsg(question);
        String result = chat.execute();
        LOG.info(() -> "Explanation result for " + output + " with question " + question + " is:\n" + result);
        if (result.contains(FIXME)) {
            System.err.println("Warning: AI returned FIXME for explain of " + output + " :\n" + result);
        }
        return result;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "AIGenerationTask{" + "inputFiles=" + inputFiles +
                ", outputFile=" + output +
                ", systemMessageFile=" + systemMessageInput +
                ", systemMessage='" + systemMessage + '\'' +
                ", promptFiles=" + promptInputs +
                ", placeholdersAndValues=" + placeholdersAndValues +
                ", prompt='" + prompt + '\'' +
                '}';
    }
}
