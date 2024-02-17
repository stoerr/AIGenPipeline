package net.stoerr.ai.aigenpipeline.framework.task;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private static final Logger LOG = Logger.getLogger(AIGenerationTask.class.getName());

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


    private final List<File> inputFiles = new ArrayList<>();
    private File outputFile;
    private String prompt;
    private File promptFile;
    private String systemMessage;
    private File systemMessageFile;

    @Override
    public AIGenerationTask clone() throws CloneNotSupportedException {
        return (AIGenerationTask) super.clone();
    }

    public AIGenerationTask addOptionalInputFile(@Nullable File file) {
        if (file != null && file.exists()) {
            inputFiles.add(file);
        } else {
            LOG.fine("Optional file not there: " + file);
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
            throw new RuntimeException("File " + file + " does not exist");
        }
        inputFiles.add(file);
        return this;
    }

    public AIGenerationTask setOutputFile(@Nonnull File file) {
        requireNonNull(file, "File must not be null");
        outputFile = file;
        return this;
    }

    protected String embedComment(String content, String comment) {
        if (outputFile.getName().endsWith(".java")) {
            return "// " + comment + "\n\n" + content;
        } else if (outputFile.getName().endsWith(".html")) {
            return content + "\n\n<!-- " + comment + " -->\n";
        } else if (outputFile.getName().endsWith(".css")) {
            return "/* " + comment + " */\n\n" + content;
        } else if (outputFile.getName().endsWith(".md")) {
            return "<!-- " + comment + " -->\n\n" + content;
        } else if (outputFile.getName().endsWith(".sh")) {
            return "# " + comment + "\n" + content;
        } else {
            return "/* " + comment + " */\n\n" + content;
        }
    }

    public boolean hasToBeRun() {
        if (!outputFile.exists()) {
            return true;
        }
        AIVersionMarker outputVersionMarker = getRecordedOutputVersionMarker();
        if (outputVersionMarker == null) {
            return true;
        }
        List<String> inputVersions = calculateAllInputsMarkers();
        List<String> oldInputVersions = outputVersionMarker.getInputVersions();
        return !new HashSet(inputVersions).equals(new HashSet(oldInputVersions));
    }

    @Nonnull
    private List<String> calculateAllInputsMarkers() {
        List<String> allInputsMarkers = new ArrayList<>();
        if (systemMessageFile != null) {
            allInputsMarkers.add(determineFileVersionMarker(systemMessageFile));
        }
        if (promptFile != null) {
            allInputsMarkers.add(determineFileVersionMarker(promptFile));
        } else {
            allInputsMarkers.add(prompt);
        }
        for (File inputFile : inputFiles) {
            String version = determineFileVersionMarker(inputFile);
            allInputsMarkers.add(version);
        }
        return allInputsMarkers;
    }

    protected String determineFileVersionMarker(@Nonnull File file) {
        String content = getFileContent(file);
        requireNonNull(content, "Could not read file " + file);
        AIVersionMarker aiVersionMarker = AIVersionMarker.find(content);
        String version;
        if (aiVersionMarker != null) {
            version = aiVersionMarker.getOurVersion();
        } else {
            version = shaHash(content);
        }
        return file.getName() + "-" + version;
    }

    protected String shaHash(String content) {
        String condensedWhitespace = content.replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(condensedWhitespace.getBytes(StandardCharsets.UTF_8));
            // turn first 4 bytes into hex number
            long hashNumber = (hash[0] & 0xFF) << 16 | (hash[1] & 0xFF) << 8 | (hash[2] & 0xFF) | (hash[3] & 0xFF) << 24;
            return Long.toHexString(hashNumber);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA256 not available", e);
        }
    }

    /**
     * Version of current output file.
     */
    protected AIVersionMarker getRecordedOutputVersionMarker() {
        if (!outputFile.exists()) {
            return null;
        }
        String content = getFileContent(outputFile);
        if (content == null) {
            return null;
        }
        AIVersionMarker aiVersionMarker = AIVersionMarker.find(content);
        if (aiVersionMarker == null) {
            throw new RuntimeException("Could not find version marker in " + outputFile);
        }
        return aiVersionMarker;
    }

    /**
     * The actual prompt to be executed. The prompt file content can contain placeholders that are replaced by the values given: placeholdersAndValues contain alternatingly placeholder names and values for them.
     *
     * @return
     */
    public AIGenerationTask setPrompt(@Nonnull File promptFile, String... placeholdersAndValues) {
        String prompt = unclutter(getFileContent(promptFile));
        requireNonNull(prompt, "Could not read prompt file " + promptFile);
        if (placeholdersAndValues.length % 2 != 0) {
            throw new RuntimeException("Odd number of placeholdersAndValues");
        }
        for (int i = 0; i < placeholdersAndValues.length; i += 2) {
            prompt = prompt.replace(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        this.prompt = prompt;
        this.promptFile = promptFile;
        return this;
    }

    protected String getFileContent(@Nonnull File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + file, e);
        }
    }

    /* Remove some clutter that is not relevant and might even confuse the AI */
    protected static String unclutter(String content) {
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
        String systemMessage = unclutter(getFileContent(systemMessageFile));
        requireNonNull(systemMessage, "Could not read system message file " + systemMessageFile);
        this.systemMessage = systemMessage;
        this.systemMessageFile = systemMessageFile;
        return this;
    }

    protected String relativePath(@Nullable File file, @Nonnull File rootDirectory) {
        if (file == null) {
            return null;
        }
        requireNonNull(rootDirectory, "Root directory must not be null");
        String rootPath = null;
        try {
            rootPath = rootDirectory.getAbsoluteFile().getCanonicalFile().getAbsolutePath();
            String filePath = file.getAbsoluteFile().getCanonicalFile().getAbsolutePath();
            if (!filePath.startsWith(rootPath)) {
                throw new RuntimeException("File " + file + " is not in root directory " + rootDirectory);
            }
            return filePath.substring(rootPath.length());
        } catch (IOException e) {
            throw new RuntimeException("Error getting canonical path for " + rootDirectory + " or " + file, e);
        }
    }

    /**
     * Execute the task if necessary. If the output file is already there and up to date, nothing is done.
     */
    public AIGenerationTask execute(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory) {
        String outputRelPath = relativePath(this.outputFile, rootDirectory);
        if (!hasToBeRun()) {
            LOG.info("Task does not have to be run for: " + outputRelPath);
            return this;
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory, outputRelPath);
        String result = chat.execute();
        LOG.fine("Result for task execution for: " + outputRelPath + "\n" + result);
        if (result.contains(FIXME)) {
            throw new RuntimeException("AI returned FIXME for " + outputRelPath + " :\n" + result);
        }
        String outputVersion = shaHash(result);
        String versionComment = new AIVersionMarker(outputVersion, calculateAllInputsMarkers()).toString();
        String withVersionComment = embedComment(result, versionComment);

        try {
            Files.write(outputFile.toPath(), withVersionComment.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error writing file " + outputFile, e);
        }
        LOG.info("Wrote file file://" + outputFile.getAbsolutePath());
        return this;
    }

    @Nonnull
    protected AIChatBuilder makeChatBuilder(@Nonnull Supplier<AIChatBuilder> chatBuilderFactory, @Nonnull File rootDirectory, String outputRelPath) {
        requireNonNull(outputFile, "No writeable output file given! " + outputFile);
        outputFile.getParentFile().mkdirs();
        if (outputFile.exists() && !outputFile.canWrite()) {
            throw new RuntimeException("No writeable output file given! " + outputFile);
        }
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("No prompt given!");
        }
        AIChatBuilder chat = chatBuilderFactory.get();
        if (systemMessage != null && !systemMessage.isBlank()) {
            chat.systemMsg(systemMessage);
        }
        inputFiles.forEach(file -> {
            // "Put it into the AI's mouth" pattern https://www.stoerr.net/blog/aimouth
            chat.userMsg("Please retrieve and print the content of " + relativePath(file, rootDirectory));
            chat.assistantMsg(unclutter(getFileContent(file)));
        });
        chat.userMsg(prompt);
        LOG.fine("Executing chat for: " + outputRelPath + "\n" + chat.toJson());
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
        String outputRelPath = relativePath(this.outputFile, rootDirectory);
        if (hasToBeRun()) { // that's not strictly necessary, but if not that's a likely mistake
            throw new RuntimeException("Task has to be already run for: " + outputRelPath);
        }
        AIChatBuilder chat = makeChatBuilder(chatBuilderFactory, rootDirectory, outputRelPath);
        String previousOutput = unclutter(getFileContent(outputFile));
        requireNonNull(previousOutput, "Could not read any content from file " + outputFile);
        chat.assistantMsg(previousOutput);
        chat.userMsg(question);
        String result = chat.execute();
        LOG.info("Explanation result for " + outputRelPath + " with question " + question + " is:\n" + result);
        if (result.contains(FIXME)) {
            throw new RuntimeException("AI returned FIXME for " + outputRelPath + " :\n" + result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "AIGenerationTask{" + "inputFiles=" + inputFiles +
                ", outputFile=" + outputFile +
                ", systemMessageFile=" + systemMessageFile +
                ", systemMessage='" + systemMessage + '\'' +
                ", promptFile=" + promptFile +
                ", prompt='" + prompt + '\'' +
                '}';
    }

}
