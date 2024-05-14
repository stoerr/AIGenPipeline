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
 *
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
    protected String explain;
    protected String url;
    protected String apiKey;
    protected String organizationId; // for OpenAI
    protected List<AIInOut> inputFiles = new ArrayList<>();
    protected List<AIInOut> promptFiles = new ArrayList<>();
    protected Map<String, String> keyValues = new LinkedHashMap<>();
    protected String model = "gpt-3.5-turbo"; // "gpt-4-turbo-preview";
    protected AIGenerationTask task = new AIGenerationTask();
    protected File rootDir = new File(".");
    protected PrintStream logStream;
    protected Integer tokens;
    protected RegenerationCheckStrategy regenerationCheckStrategy = RegenerationCheckStrategy.VERSIONMARKER;
    protected WritingStrategy writingStrategy = WritingStrategy.WITHVERSION;
    protected String writePart;
    protected boolean printconfig;
    protected String infilePromptMarker;
    protected String outputScan;

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
            ERR.println("Usage error: " + e.getMessage());
            OUT.flush();
            ERR.flush();
            if (verbose) {
                e.printStackTrace();
            }
//            System.err.println();
//            printHelpAndExit(true);
            System.exit(1);
        }
    }

    public AIChatBuilder makeChatBuilder() {
        AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();
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
        this.logStream = output == null || output.isBlank() ? OUT : ERR;
        File outputFile = Path.of(".").resolve(requireNonNull(output, "No output file given.")).toFile();
        if (infilePromptMarker != null) {
            String[] separators = SegmentedFile.infilePrompting(infilePromptMarker);
            SegmentedFile segmentedFile = new SegmentedFile(outputFile, separators);
            task.addPrompt(AIInOut.of(segmentedFile, 1));
            task.setOutput(AIInOut.of(segmentedFile, 3));
        } else if (writePart != null) {
            SegmentedFile segmentedFile = new SegmentedFile(outputFile, writePart, writePart);
            task.setOutput(AIInOut.of(segmentedFile, 1));
        } else {
            task.setOutput(AIInOut.of(outputFile));
        }
        for (AIInOut inputFile : inputFiles) {
            File file = inputFile.getFile();
            if (file.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                task.addOptionalInput(inputFile);
            } else {
                task.addInput(inputFile);
            }
        }
        promptFiles.stream()
                .forEach(f -> task.addPrompt(f, keyValues));
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
        if (explain != null && !explain.isBlank()) {
            String explanation = task.explain(this::makeChatBuilder, rootDir, explain);
            OUT.println(explanation);
        } else {
            task.execute(this::makeChatBuilder, rootDir);
        }
    }

    /**
     * Scans for files in {@link #outputScan} and processes them.
     */
    protected void runWithOutputScan(String[] args) {
        FileLookupHelper helper = FileLookupHelper.fromPath(".");
        List<File> files = helper.filesContaining(".", outputScan, SegmentedFile.REGEX_AIGENPROMPTSTART, true);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files with AIGenPromptStart found for pattern " + outputScan);
        } else if (verbose) {
            OUT.println("Found " + files.size() + " files: " + files);
        }
        for (File file : files) {
            String content = AIInOut.of(file).read();
            Matcher promptStartMatch = SegmentedFile.REGEX_AIGENPROMPTSTART.matcher(content);
            while (promptStartMatch.find()) {
                String marker = promptStartMatch.group("id");
                try {
                    SegmentedFile segmentedFile = new SegmentedFile(file, SegmentedFile.infilePrompting(marker));
                    String infileArguments = segmentedFile.getSegment(2);
                    parseArguments(infileArguments.split("\\s+"), file.getParentFile());
                    AIGenPipeline subPipeline = new AIGenPipeline();
                    List<String> subArgs = new ArrayList<>(Arrays.asList(args));
                    subArgs.addAll(List.of("-ifp", marker, file.getAbsolutePath()));
                    subPipeline.readArguments(subArgs.toArray(new String[0]), rootDir);
                    subPipeline.rootDir = file.getParentFile();
                    subPipeline.run();
                } catch (IOException e) {
                    ERR.println("Error processing file " + file + ": " + e);
                }
            }
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
            String infileArguments = segmentedFile.getSegment(2);
            parseArguments(infileArguments.split("\\s+"), outputFile.getParentFile());
        }
    }

    protected List<AIGenArgumentList> collectArgLists(String[] args, File startDir) {
        List<AIGenArgumentList> argLists = new ArrayList<>();

        AIGenArgumentList argsConfig = new AIGenArgumentList(args);
        argLists.add(argsConfig);

        // If no -cn option is given, scan for .aigenpipeline files upwards from the output file directory.
        if (isContinueScan(argsConfig)) {
            File currentDir = startDir;
            while (currentDir != null) {
                File configFile = new File(currentDir, CONFIGFILE);
                if (configFile.exists()) {
                    AIGenArgumentList argumentsFromFile = new AIGenArgumentList(configFile);
                    argLists.add(argumentsFromFile);
                    if (!isContinueScan(argumentsFromFile)) break;
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

    protected boolean isContinueScan(AIGenArgumentList args) {
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
                case "-os":
                case "--outputscan":
                    outputScan = args[++i];
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
                    Path inputArg = dir.toPath().resolve(args[i]);
                    inputFiles.add(AIInOut.of(inputArg));
                    break;
            }
        }
    }

    /**
     * This reads the collected texts of the website from /helpaitexts.md and gives them to the AI, and then has it
     * answer the #helpAIquestion from that.
     */
    protected void answerHelpAIQuestion() throws IOException {
        StringBuilder helptext = new StringBuilder();
        try (InputStream is = AIGenPipeline.class.getResourceAsStream("/helpaitexts.md")) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                helptext.append(scanner.nextLine());
                helptext.append("\n");
            }
        }
        try {
            OUT.println("Trying to get an answer from the AI...\n");
            AIChatBuilder aiChatBuilder = makeChatBuilder();
            aiChatBuilder.userMsg("Print the collected help texts for the aigenpipeline tool.");
            aiChatBuilder.assistantMsg(helptext.toString());
            aiChatBuilder.userMsg("From this help texts, please answer the following question:\n\n" + helpAIquestion);
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
        try (InputStream helpfile = getClass().getResourceAsStream("aigencmdline/usage.txt");
             InputStreamReader reader = new InputStreamReader(helpfile, StandardCharsets.UTF_8)) {
            Writer writer = new StringWriter();
            reader.transferTo(writer);
            (onerror ? ERR : OUT).println(writer.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Bug: cannot read usage file.");
        }
    }

}
