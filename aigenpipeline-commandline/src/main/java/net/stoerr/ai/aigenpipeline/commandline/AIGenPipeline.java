package net.stoerr.ai.aigenpipeline.commandline;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.CopyPseudoAIChatBuilderImpl;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;
import net.stoerr.ai.aigenpipeline.framework.task.AIGenerationTask;
import net.stoerr.ai.aigenpipeline.framework.task.AIInOut;
import net.stoerr.ai.aigenpipeline.framework.task.FileLookupHelper;
import net.stoerr.ai.aigenpipeline.framework.task.RegenerationCheckStrategy;
import net.stoerr.ai.aigenpipeline.framework.task.SegmentedFile;
import net.stoerr.ai.aigenpipeline.framework.task.WritingStrategy;

/**
 * <p>
 * The main entry point of the AI Generation Pipeline.
 * </p>
 * <p>
 * This class reads the command line arguments, reads the configuration files, and then executes the AI generation task.
 * </p>
 */
public class AIGenPipeline {

    /**
     * Name of the environment variable where we read common configuration from.
     */
    public static final String AIGENPIPELINE_CONFIG = "AIGENPIPELINE_CONFIG";

    /**
     * Name of configuration files we scan upwards from the output directory.
     */
    public static final String CONFIGFILE = ".aigenpipeline";
    public static final PrintStream OUT = System.out;
    public static final PrintStream ERR = System.err;

    /**
     * Pattern for accessing an environment variable, e.g. $FOO or ${FOO}
     */
    protected final Pattern ENVVARIABLE_PATTERN = Pattern.compile("\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\{[A-Za-z_][A-Za-z0-9_]*\\}");

    protected boolean help, verbose, dryRun, check, version;
    protected String helpAIquestion;
    protected String output;
    protected AIInOut taskOutput;
    protected String explain;
    protected String url;
    protected String apiKey;
    protected String organizationId; // for OpenAI
    protected List<AIInOut> inputFiles = new ArrayList<>();
    protected List<AIInOut> promptFiles = new ArrayList<>();
    protected Map<String, String> keyValues = new LinkedHashMap<>();
    protected String model = "gpt-4o-mini"; // "gpt-4o";
    protected AIGenerationTask task = new AIGenerationTask();
    protected File rootDir = new File(".");
    protected PrintStream logStream = System.err;
    protected Integer tokens;
    protected RegenerationCheckStrategy regenerationCheckStrategy = RegenerationCheckStrategy.VERSIONMARKER;
    protected WritingStrategy writingStrategy = WritingStrategy.WITHVERSION;
    protected String writePart;
    protected boolean printconfig;
    protected String infilePromptMarker;
    protected String outputScan;
    protected boolean printdependencydiagram;
    protected boolean update;
    protected List<AIInOut> hintFiles = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new AIGenPipeline().run(args);
    }

    protected void run(String[] args) throws IOException {
        try {
            readArguments(args, rootDir);

            if (version) {
                OUT.println(getVersion());
            }
            if (help || args.length == 0) {
                printHelp(false);
            }
            if (helpAIquestion != null) {
                answerHelpAIQuestion();
            }
            if (version || help || helpAIquestion != null || printconfig) {
                OUT.flush();
                ERR.flush();
                System.exit(0);
            }

            if (outputScan == null) {
                run();
            } else {
                runWithOutputScan(args);
            }
        } catch (IllegalArgumentException e) {
            logStream.println("Usage error for " + output + " : " + e.getMessage());
            if (verbose) {
                e.printStackTrace(ERR);
            }
            OUT.flush();
            ERR.flush();
            System.exit(1);
        }
    }

    public AIChatBuilder makeChatBuilder() {
        AIChatBuilder chatBuilder =
                CopyPseudoAIChatBuilderImpl.MODEL_COPY.equals(model) ?
                        new CopyPseudoAIChatBuilderImpl() :
                        new OpenAIChatBuilderImpl();
        if (null != url) {
            chatBuilder.url(url);
        }
        if (null != apiKey) {
            chatBuilder.key(apiKey);
        }
        if (null != organizationId) {
            chatBuilder.organizationId(organizationId);
        }
        if (null != model) {
            chatBuilder.model(model);
        }
        if (null != tokens) {
            chatBuilder.maxTokens(tokens);
        }
        return chatBuilder;
    }

    protected void run() throws IOException {
        prepareTask();
        executeTask();
    }

    protected void prepareTask() throws IOException {
        this.logStream = output == null || output.isBlank() ? OUT : ERR;
        File outputFile = Path.of(".").resolve(requireNonNull(output, "No output file given.")).toFile();
        if (infilePromptMarker != null) {
            String[] separators = SegmentedFile.infilePrompting(infilePromptMarker);
            SegmentedFile segmentedFile = new SegmentedFile(outputFile, separators);
            task.addPrompt(AIInOut.of(segmentedFile, 1));
            taskOutput = AIInOut.of(segmentedFile, 3);
        } else if (writePart != null) {
            SegmentedFile segmentedFile = new SegmentedFile(outputFile, writePart, writePart);
            taskOutput = AIInOut.of(segmentedFile, 1);
        } else {
            taskOutput = AIInOut.of(outputFile);
        }
        task.setOutput(taskOutput);
        for (AIInOut inputFile : inputFiles) {
            if (inputFile.sameFile(taskOutput)) {
                task.addOptionalInput(inputFile);
            } else {
                task.addInput(inputFile);
            }
        }
        hintFiles.forEach(task::addHint);
        task.setUpdateRequested(update);
        promptFiles.forEach(f -> task.addPrompt(f, keyValues));
        task.setRegenerationCheckStrategy(regenerationCheckStrategy);
        task.setWritingStrategy(writingStrategy);
        if (check) {
            boolean hasToBeRun = task.hasToBeRun();
            if (verbose) {
                logStream.println("Needs running: " + hasToBeRun);
            }
            System.exit(hasToBeRun ? 0 : 1); // command line like: 0 is "OK" = file is up to date.
        }
        if (verbose) {
            logStream.println(task.toJson(this::makeChatBuilder, rootDir));
        }
        if (dryRun) {
            boolean hasToBeRun = task.hasToBeRun();
            logStream.println("Dryrun - not executed; needs executing: " + hasToBeRun);
            return;
        }
        if (explain != null) {
            String explanation = task.explain(this::makeChatBuilder, rootDir, explain);
            OUT.println(explanation);
        }
    }

    protected void executeTask() {
        try {
            if (printdependencydiagram) {
                new AIDepDiagram(Arrays.asList(this), rootDir).printDepDiagram(logStream);
                return;
            }
            if (explain == null) {
                if (verbose) logStream.println("Executing task for " + taskOutput.getFile());
                task.execute(this::makeChatBuilder, rootDir);
            }
        } catch (RuntimeException e) {
            String outputLocation = taskOutput != null ? taskOutput.getFile().getPath() : output;
            logStream.println("Error regarding " + outputLocation);
            throw e;
        }
    }

    /**
     * Scans for files in {@link #outputScan} and processes them.
     * @param args the command line arguments
     */
    protected void runWithOutputScan(String[] args) {
        if (!inputFiles.isEmpty()) {
            throw new IllegalArgumentException("Cannot use -os with additional input files. " +
                    "(Likely a misusage - did you forget to quote the pattern on the command line?) " +
                    "Actual arguments were: " + Arrays.toString(args));
        }
        FileLookupHelper helper = FileLookupHelper.fromPath(".");
        List<File> files = helper.filesContaining(".", outputScan, SegmentedFile.REGEX_AIGENPROMPTSTART, true);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files with AIGenPromptStart found for pattern " + outputScan);
        } else if (verbose) {
            OUT.println("Found " + files.size() + " files: " + files);
        }
        List<AIGenPipeline> subPipelines = new ArrayList<>();
        for (File file : files) {
            String content = AIInOut.of(file).read();
            Matcher promptStartMatch = SegmentedFile.REGEX_AIGENPROMPTSTART.matcher(content);
            while (promptStartMatch.find()) {
                String marker = promptStartMatch.group("id");
                try {
                    SegmentedFile segmentedFile = new SegmentedFile(file, SegmentedFile.infilePrompting(marker));
                    String infileArguments = segmentedFile.getSegment(2).trim().replaceAll("\\s+", " ");
                    if (verbose) {
                        logStream.println("Processing file " + file + " with marker " + marker + " and arguments " + infileArguments);
                    }
                    parseArguments(infileArguments.split("\\s+"), file.getParentFile());
                    AIGenPipeline subPipeline = new AIGenPipeline();
                    List<String> subArgs = new ArrayList<>(Arrays.asList(args));
                    subArgs.addAll(List.of("-ifp", marker, file.getAbsolutePath()));
                    subPipeline.readArguments(subArgs.toArray(new String[0]), rootDir);
                    subPipeline.rootDir = file.getParentFile();
                    subPipeline.prepareTask();
                    subPipelines.add(subPipeline);
                } catch (IOException e) {
                    ERR.println("Error processing file " + file + ": " + e);
                }
            }
        }
        if (verbose) {
            OUT.println("Processing " + subPipelines.size() + " tasks.");
        }
        if (printdependencydiagram) {
            new AIDepDiagram(subPipelines, rootDir).printDepDiagram(logStream);
        } else {
            List<AIGenPipeline> sorted = new AIDepDiagram(subPipelines, rootDir).sortedPipelines();
            sorted.forEach(AIGenPipeline::executeTask);
        }
    }

    protected void readArguments(String[] args, @Nonnull File startDir) throws IOException {
        List<AIGenArgumentList> argLists = collectArgLists(args, startDir);
        for (AIGenArgumentList argumentsFromFile : argLists) {
            parseArguments(argumentsFromFile.getArgs(), startDir);
        }

        if (printconfig) {
            OUT.println("Collected argument lists from environment, configuration files and arguments:");
            for (AIGenArgumentList argumentsFromFile : argLists) {
                File configFile = argumentsFromFile.getCfgFile();
                if (null != configFile) {
                    OUT.println(configFile.getAbsolutePath());
                }
                OUT.println(Arrays.toString(argumentsFromFile.getArgs()));
            }
        }

        if (infilePromptMarker != null) {
            File outputFile = startDir.toPath().resolve(
                    requireNonNull(output, "No output file given.")).toFile();
            String[] separators = SegmentedFile.infilePrompting(infilePromptMarker);
            SegmentedFile segmentedFile = new SegmentedFile(outputFile, separators);
            String infileArguments = segmentedFile.getSegment(2).trim().replaceAll("\\s+", " ");
            parseArguments(infileArguments.split("\\s+"), outputFile.getParentFile());
        }
    }

    protected List<AIGenArgumentList> collectArgLists(String[] args, File startDir) {
        List<AIGenArgumentList> argLists = new ArrayList<>();

        AIGenArgumentList argsConfig = new AIGenArgumentList(args);
        argLists.add(argsConfig);

        // If no -cn option is given, scan for .aigenpipeline files upwards from the output file directory.
        if (!isStopCfgfileScan(argsConfig)) {
            File currentDir = startDir;
            while (currentDir != null) {
                File configFile = new File(currentDir, CONFIGFILE);
                if (configFile.exists()) {
                    AIGenArgumentList argumentsFromFile = new AIGenArgumentList(configFile);
                    argLists.add(argumentsFromFile);
                    if (!isStopCfgfileScan(argumentsFromFile)) break;
                }
                currentDir = currentDir.getParentFile();
            }
        }

        // read the arguments from the environment variable only if no -cne option was given
        boolean ignoreEnvironmentArgs = argLists.stream().anyMatch(this::isIgnoreEnvironmentArgs);
        if (!ignoreEnvironmentArgs) {
            AIGenArgumentList envConfig = new AIGenArgumentList(System.getenv(AIGENPIPELINE_CONFIG) == null ? new String[0] :
                    System.getenv(AIGENPIPELINE_CONFIG).split("\\s+"));
            argLists.add(envConfig);
        }

        Collections.reverse(argLists);
        return argLists;
    }

    protected boolean isStopCfgfileScan(AIGenArgumentList args) {
        return args.hasArgument("-cn", "--confignoscan");
    }

    protected boolean isIgnoreEnvironmentArgs(AIGenArgumentList args) {
        return args.hasArgument("-cne", "--configignoreenv");
    }

    protected void parseArguments(String[] args, File dir) throws IOException {
        // replace environment variables
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("$")) {
                Matcher matcher = ENVVARIABLE_PATTERN.matcher(args[i]);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String env = System.getenv(matcher.group().substring(1));
                    matcher.appendReplacement(sb, env != null ? env : "");
                }
                matcher.appendTail(sb);
                args[i] = sb.toString();
            }
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    help = true;
                    break;
                case "-ha":
                case "--help-ai":
                    helpAIquestion = args[++i];
                    break;
                case "--version":
                    version = true;
                    break;
                case "-o":
                case "--output":
                    if (output != null) {
                        throw new IllegalArgumentException("Output file already given: " + output);
                    }
                    output = args[++i];
                    break;
                case "--hint":
                    String hintFileName = args[++i];
                    if (hintFileName.equals("-")) {
                        hintFiles.add(AIInOut.of(System.in));
                    } else {
                        hintFiles.add(AIInOut.of(new File(hintFileName)));
                    }
                    break;
                case "-upd":
                case "--update":
                    update = true;
                    break;
                case "-os":
                case "--outputscan":
                    outputScan = args[++i];
                    break;
                case "-dd":
                case "--dependencydiagram":
                    printdependencydiagram = true;
                    break;
                case "-p":
                case "--prompt":
                    AIInOut path = AIInOut.of(dir.toPath().resolve(Path.of(args[++i])));
                    promptFiles.add(path);
                    break;
                case "-ifp":
                case "--infileprompt":
                    infilePromptMarker = args[++i];
                    output = args[++i];
                    break;
                case "-k":
                    String[] kv = args[++i].split("=", 2);
                    keyValues.put(kv[0], kv[1]);
                    break;
                case "-s":
                case "--sysmsg":
                    task.setSystemMessage(new File(args[++i]));
                    break;
                case "-v":
                case "--verbose":
                    verbose = true;
                    break;
                case "-n":
                case "--dry-run":
                    dryRun = true;
                    break;
                case "-c":
                case "--check":
                    check = true;
                    break;
                case "-f":
                case "--force":
                case "-ga":
                case "--gen-always":
                    regenerationCheckStrategy = RegenerationCheckStrategy.ALWAYS;
                    break;
                case "-gn":
                case "--gen-notexists":
                    regenerationCheckStrategy = RegenerationCheckStrategy.IF_NOT_EXISTS;
                    break;
                case "-go":
                case "--gen-older":
                    regenerationCheckStrategy = RegenerationCheckStrategy.IF_OLDER;
                    break;
                case "-gv":
                case "--gen-versioncheck":
                    regenerationCheckStrategy = RegenerationCheckStrategy.VERSIONMARKER;
                    break;
                case "-wv":
                case "--write-version":
                    writingStrategy = WritingStrategy.WITHVERSION;
                    break;
                case "-wo":
                case "--write-noversion":
                    writingStrategy = WritingStrategy.WITHOUTVERSION;
                    break;
                case "-wp":
                case "--write-part":
                    writePart = args[++i];
                    break;
                case "-e":
                case "--explain":
                    explain = args[++i];
                    break;
                case "-u":
                case "--url":
                    url = args[++i];
                    break;
                case "-a":
                case "--api-key":
                    apiKey = args[++i];
                    break;
                case "-org":
                case "--organization":
                    organizationId = args[++i];
                    break;
                case "-m":
                case "--model":
                    model = args[++i];
                    break;
                case "-t":
                case "--maxtokens":
                    tokens = Integer.parseInt(args[++i]);
                    break;
                case "-cf":
                case "--configfile":
                    String filename = args[++i];
                    Path cfgFilePath = dir.toPath().resolve(filename);
                    AIGenArgumentList cfgFileArgs = new AIGenArgumentList(cfgFilePath.toFile());
                    parseArguments(cfgFileArgs.getArgs(), cfgFilePath.getParent().toFile());
                    break;
                case "-cp":
                case "--configprint":
                    printconfig = true;
                    break;
                case "-cn":
                case "--confignoscan":
                case "-cne":
                case "--configignoreenv":
                    // handled when reading config files, just ignore here
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + args[i]);
                    }
                    if (explain != null) {
                        explain += " " + args[i];
                    } else if (helpAIquestion != null) {
                        helpAIquestion += " " + args[i];
                    } else {
                        Path inputArg = dir.toPath().resolve(args[i]);
                        inputFiles.add(AIInOut.of(inputArg));
                    }
                    break;
            }
        }
    }

    /**
     * This reads the collected texts of the website from /helpaitexts.md and gives them to the AI, and then has it
     * answer the #helpAIquestion from that.
     * @throws IOException if the help texts could not be read
     */
    protected void answerHelpAIQuestion() throws IOException {
        StringBuilder helptext = new StringBuilder();
        try (InputStream is = AIGenPipeline.class.getResourceAsStream("/helpaitexts.md")) {
            Scanner scanner = new Scanner(requireNonNull(is), StandardCharsets.UTF_8);
            while (scanner.hasNextLine()) {
                helptext.append(scanner.nextLine());
                helptext.append("\n");
            }
        }
        try {
            OUT.println("Trying to get an answer from the AI...\n");
            AIChatBuilder aiChatBuilder = makeChatBuilder();
            aiChatBuilder.systemMsg("You are a helper for the AI based code generation pipeline. " +
                    "You answer the users question about it from the collected help texts." +
                    "Answer in plain text!");
            aiChatBuilder.userMsg("Print the collected help texts for the aigenpipeline tool.");
            aiChatBuilder.assistantMsg(helptext.toString());
            aiChatBuilder.userMsg("From this help texts, please answer the following question:\n\n" + helpAIquestion);
            if (verbose) logStream.println("Asking AI:\n" + aiChatBuilder.toJson());
            String answer = aiChatBuilder.execute();
            OUT.println(answer);
        } catch (Exception e) {
            OUT.println("Failed to get an answer from the AI, possibly because of missing configuration: " + e);
            OUT.println("You need a working access to an AI service to get an answer from the AI.");
            OUT.println("Here are the web pages describing the tool:\n");
            OUT.println(helptext);
            OUT.println("\n");
            printHelp(false);
            OUT.println("You can repeat asking your question if you give keys etc. to have access to an AI service.\n");
            OUT.println("Failed to get an answer from the AI, possibly because of missing configuration: " + e);
        }
    }

    protected String getVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(AIGenPipeline.class.getResourceAsStream("/git.properties"));
        return "AIGenPipeline Version " + properties.get("git.build.version") + "-" +
                properties.getProperty("git.commit.id.describe") + " from " + properties.getProperty("git.build.time");
    }

    protected void printHelp(boolean onerror) {
        try (InputStream usageFile = getClass().getResourceAsStream("/aigencmdline/usage.txt");
             InputStreamReader reader = new InputStreamReader(
                     requireNonNull(usageFile), StandardCharsets.UTF_8)) {
            Writer writer = new StringWriter();
            reader.transferTo(writer);
            (onerror ? ERR : OUT).println(writer);
        } catch (IOException e) {
            throw new IllegalStateException("Bug: cannot read usage file.");
        }
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AIGenPipeline{");
        sb.append("output='").append(output).append('\'');
        sb.append(", taskOutput=").append(taskOutput);
        sb.append(", inputFiles=").append(inputFiles);
        sb.append(", promptFiles=").append(promptFiles);
        sb.append(", writePart='").append(writePart).append('\'');
        sb.append(", infilePromptMarker='").append(infilePromptMarker).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
