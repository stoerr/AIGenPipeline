package net.stoerr.ai.aigenpipeline.commandline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;
import net.stoerr.ai.aigenpipeline.framework.task.AIGenerationTask;
import net.stoerr.ai.aigenpipeline.framework.task.RegenerationCheckStrategy;
import net.stoerr.ai.aigenpipeline.framework.task.WritingStrategy;

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
    protected String output, explain;
    protected String url;
    protected String apiKey;
    protected String organizationId; // for OpenAI
    protected List<String> inputFiles = new ArrayList<>();
    protected List<String> promptFiles = new ArrayList<>();
    protected Map<String, String> keyValues = new LinkedHashMap<>();
    protected String model = "gpt-3.5-turbo"; // "gpt-4-turbo-preview";
    protected AIGenerationTask task;
    protected File rootDir = new File(".");
    protected PrintStream logStream;
    protected Integer tokens;
    protected RegenerationCheckStrategy regenerationCheckStrategy = RegenerationCheckStrategy.VERSIONMARKER;
    protected WritingStrategy writingStrategy = WritingStrategy.WITHVERSION;
    protected boolean printconfig;

    public static void main(String[] args) throws IOException {
        new AIGenPipeline().run(args);
    }

    protected void run(String[] args) throws IOException {
        task = new AIGenerationTask();

        try {
            readArguments(args, new File("."));

            if (version) {
                OUT.println(getVersion());
            }
            if (help || args.length == 0) {
                printHelp(false);
            }
            if (helpAIquestion != null) {
                answerHelpAIQuestion();
            }
            OUT.flush();
            ERR.flush();
            if (version || help || helpAIquestion != null || printconfig) {
                System.exit(0);
            }

            run();
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

    protected void readArguments(String[] args, @Nonnull File startDir) throws IOException {
        String[] envargs = System.getenv(AIGENPIPELINE_CONFIG) == null ? new String[0] :
                System.getenv(AIGENPIPELINE_CONFIG).split("\\s+");
        List<String[]> argumentSets = new ArrayList<>();
        List<File> configFiles = new ArrayList<>();
        argumentSets.add(args);

        // If no -cn option is given, scan for .aigenpipeline files upwards from the output file directory.
        if (isContinueScan(args)) {
            File currentDir = startDir;
            while (currentDir != null) {
                File configFile = new File(currentDir, CONFIGFILE);
                if (configFile.exists()) {
                    String[] argumentsFromFile = parseConfigFile(configFile.getAbsolutePath());
                    argumentSets.add(argumentsFromFile);
                    configFiles.add(configFile);
                    if (!isContinueScan(argumentsFromFile)) break;
                }
                currentDir = currentDir.getParentFile();
            }
        }

        // read the arguments from the environment variable only if no -cne option was given
        boolean useEnvironmentArgs = argumentSets.stream().anyMatch(this::isReadEnvironmentVariableArguments);
        if (useEnvironmentArgs) {
            argumentSets.add(envargs);
        }

        Collections.reverse(argumentSets);
        for (String[] argumentsFromFile : argumentSets) {
            parseArguments(argumentsFromFile);
        }

        if (printconfig) {
            Collections.reverse(configFiles);
            if (useEnvironmentArgs) {
                OUT.println("Arguments from environment:\n" + Arrays.toString(argumentSets.get(0)));
                argumentSets.remove(0);
            }
            for (File configFile : configFiles) {
                OUT.println(configFile.getAbsolutePath());
                OUT.println(Arrays.toString(argumentSets.get(0)));
                argumentSets.remove(0);
            }
            OUT.println("Arguments from command line:\n" + Arrays.toString(argumentSets.get(0)));
            OUT.println(argumentSets.size());
        }
    }

    protected boolean isContinueScan(String[] argumentsFromFile) {
        return null == argumentsFromFile || !Arrays.asList(argumentsFromFile).contains("-cn")
                && !Arrays.asList(argumentsFromFile).contains("--confignoscan");
    }

    protected boolean isReadEnvironmentVariableArguments(String[] argumentsFromFile) {
        return null == argumentsFromFile || !Arrays.asList(argumentsFromFile).contains("-cne")
                && !Arrays.asList(argumentsFromFile).contains("--configignoreenv");
    }

    protected String[] parseConfigFile(String filename) throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(filename), StandardCharsets.UTF_8)) {
            String content = lines
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#"))
                    .collect(Collectors.joining(" "));
            return content.split("\\s+");
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
        File outputFile = toFile(Objects.requireNonNull(output, "No output file given."));
        task.setOutputFile(outputFile);
        for (String inputFile : inputFiles) {
            File file = toFile(inputFile);
            if (file.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                task.addOptionalInputFile(file);
            } else {
                task.addInputFile(file);
            }
        }
        promptFiles.stream()
                .map(this::toFile)
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

    protected File toFile(String filename) {
        return rootDir.toPath().toAbsolutePath().relativize(
                Path.of(filename).toAbsolutePath()).toFile();
    }

    protected void printHelp(boolean onerror) {
        (onerror ? ERR : OUT).println("" +
                "Usage:\n" +
                "aigenpipeline [options] [<input_files>...]\n" +
                "\n" +
                "Options:\n" +
                "\n" +
                "  General options:\n" +
                "    -h, --help               Show this help message and exit.\n" +
                "    -ha, --helpai <question> Answer a question about the tool from the text on its documentation site and exit.\n" +
                "    --version                Show the version of the AIGenPipeline tool and exit.\n" +
                "    -c, --check              Only check if the output needs to be regenerated based on input versions without actually \n" +
                "                             generating it. The exit code is 0 if the output is up to date, 1 if it needs to be \n" +
                "                             regenerated.\n" +
                "    -n, --dry-run            Enable dry-run mode, where the tool will only print to stderr what it would do without \n" +
                "                             actually calling the AI or writing any files.\n" +
                "    -v, --verbose            Enable verbose output to stderr, providing more details about the process.\n" +
                "\n" +
                "  Input / outputs:\n" +
                "    -o, --output <file>      Specify the output file where the generated content will be written. Mandatory.\n" +
                "    -p, --prompt <file>      Reads a prompt from the given file.\n" +
                "    -s, --sysmsg <file>      Optional: Reads a system message from the given file instead of using the default. \n" +
                "    -k <key>=<value>         Sets a key-value pair replacing ${key} in prompt files with the value. \n" +
                "\n" +
                "  AI Generation control:\n" +
                "    -f, --force              Force regeneration of output files, ignoring any version checks - same as -ga.\n" +
                "    -ga, --gen-always        Generate the output file always, ignoring version checks.\n" +
                "    -gn, --gen-notexists     Generate the output file only if it does not exist.\n" +
                "    -go, --gen-older         Generate the output file if it does not exist or is older than any of the input files.\n" +
                "    -gv, --gen-versioncheck  Generate the output file if the version of the input files has changed. (Default.)\n" +
                "    -wv, --write-version     Write the output file with a version comment. (Default.)\n" +
                "    -wo, --write-noversion   Write the output file without a version comment. Not compatible with default -gv .\n" +
                "    -wp, --write-part <marker> Replace the lines between the first occurrence of the marker and the second occurrence." +
                "                             If a version marker is written, it has to be in the first of those lines and is changed there." +
                "                             It is an error if the marker does not occur exactly twice; the output file has to exist.\n" +
                "    -e, --explain <question> Asks the AI a question about the generated result. This needs _exactly_the_same_command_line_\n" +
                "                             that was given to generate the output file, and the additional --explain <question> option.\n" +
                "                             It recreates the conversation that lead to the output file and asks the AI for a \n" +
                "                             clarification. The output file is not written, but read to recreate the conversation.\n" +
                "\n" +
                "  Configuration files:\n" +
                "    -cf, --configfile <file> Read configuration from the given file. These contain options like on the command line.\n" +
                "    -cn, --confignoscan      Do not scan for `.aigenpipeline` config files.\n" +
                "    -cne, --configignoreenv  Ignore the environment variable `AIGENPIPELINE_CONFIG`.\n" +
                "    -cp, --configprint       Print the collected configurations and exit.\n" +
                "\n" +
                "  AI backend settings:\n" +
                "    -u, --url <url>          The URL of the AI server. Default is https://api.openai.com/v1/chat/completions .\n" +
                "                             In the case of OpenAI the API key is expected to be in the environment variable \n" +
                "                             OPENAI_API_KEY, or given with the -a option.\n" +
                "    -a, --api-key <key>      The API key for the AI server. If not given, it's expected to be in the environment variable \n" +
                "                             OPENAI_API_KEY, or you could use a -u option to specify a different server that doesnt need\n" +
                "                             an API key. Used in \"Authorization: Bearer <key>\" header.\n" +
                "    -org, --organization <id> The optional organization id in case of the OpenAI server.\n" +
                "    -m, --model <model>      The model to use for the AI. Default is gpt-4-turbo-preview .\n" +
                "    -t <maxtokens>           The maximum number of tokens to generate.\n" +
                "\n" +
                "Arguments:\n" +
                "  [<input_files>...]       Input files to be processed into the output file. \n" +
                "\n" +
                "Examples:\n" +
                "  Generate documentation from a prompt file:\n" +
                "    aigenpipeline -p prompts/documentation_prompt.txt -o generated_documentation.md src/foo/bar.java src/foo/baz.java\n" +
                "\n" +
                "  Force regenerate an interface from an OpenAPI document, ignoring version checks:\n" +
                "    aigenpipeline -f -o specs/openapi.yaml -p prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java\n" +
                "\n" +
                "  Ask how to improve a prompt after viewing the initial generation of specs/openapi.yaml:\n" +
                "    aigenpipeline -o PreviousOutput.java -p prompts/promptGenertaion.txt specs/openapi.yaml --explain \"Why did you not use annotations?\"  \n" +
                "\n" +
                "Configuration files:\n" +
                "  These contain options like on the command line. The environment variable `AIGENPIPELINE_CONFIG` can contain options.\n" +
                "  If -cn is not given, the tool scans for files named .aigenpipeline upwards from the output file directory.\n" +
                "  The order these configurations are processed is: environment variable, .aigenpipeline files from top to bottom,\n" +
                "  command line arguments. Thus the later override the earlier one, as these get more specific to the current call.\n" +
                "  Lines starting with a # are ignored in configuration files (comments).\n" +
                "\n" +
                "Note:\n" +
                "  It's recommended to manually review and edit generated files. Use version control to manage and track changes over time. \n" +
                "  More detailed instructions and explanations can be found at https://aigenpipeline.stoerr.net/ .\n"
        );
    }

    protected void parseArguments(String[] args) throws IOException {
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
                    output = args[++i];
                    break;
                case "-p":
                case "--prompt":
                    promptFiles.add(args[++i]);
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
                    writingStrategy = new WritingStrategy.WritePartStrategy(args[++i]);
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
                    parseArguments(parseConfigFile(filename));
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
                    inputFiles.add(args[i]);
                    break;
            }
        }
    }

    protected String getVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(AIGenPipeline.class.getResourceAsStream("/git.properties"));
        return "AIGenPipeline Version " + properties.get("git.build.version") + "-" +
                properties.getProperty("git.commit.id.describe") + " from " + properties.getProperty("git.build.time");
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

}
