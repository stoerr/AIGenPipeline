package net.stoerr.ai.aigenpipeline.commandline;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;
import net.stoerr.ai.aigenpipeline.framework.task.AIGenerationTask;

public class AIGenPipeline {

    protected boolean help, verbose, dryRun, check, force, version;
    protected String output, explain;
    protected String url;
    protected String key;
    protected List<String> inputFiles = new ArrayList<>();
    protected List<String> promptFiles = new ArrayList<>();
    protected String model = "gpt-4-turbo-preview";
    protected AIGenerationTask task;
    protected File rootDir = new File(".");
    protected PrintStream logStream;
    protected Integer tokens;


    public static void main(String[] args) throws IOException {
        new AIGenPipeline().run(args);
    }

    protected void run(String[] args) throws IOException {
        parseArguments(args);
        if (version) {
            System.out.println(getVersion());
        }
        if (help) {
            printHelpAndExit(false);
        }
        if (version || help) {
            System.exit(0);
        }
        run();
    }

    public AIChatBuilder makeChatBuilder() {
        AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();
        if (null != url) {
            chatBuilder.url(url);
        }
        if (null != key) {
            chatBuilder.key(key);
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
        this.logStream = output == null || output.isBlank() ? System.out : System.err;
        task = new AIGenerationTask();
        inputFiles.stream().map(this::toFile).forEach(task::addInputFile);
        task.setOutputFile(toFile(Objects.requireNonNull(output, "No output file given.")));
        if (promptFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one prompt file has to be given.");
        }
        promptFiles.stream().map(this::toFile).forEach(task::addPrompt);
        task.force(force);
        if (verbose) {
            logStream.println(task.toJson(this::makeChatBuilder, rootDir));
        }
        if (check) {
            boolean hasToBeRun = task.hasToBeRun();
            if (verbose) {
                logStream.println("Needs running: " + hasToBeRun);
            }
            System.exit(hasToBeRun ? 0 : 1); // command line like: 0 is "OK" = file is up to date.
        }
        if (dryRun) {
            logStream.println("Dryrun - not executed.");
            return;
        }
        if (explain != null && !explain.isBlank()) {
            String explanation = task.explain(this::makeChatBuilder, rootDir, explain);
            System.out.println(explanation);
        } else {
            task.execute(this::makeChatBuilder, rootDir);
        }
    }

    protected File toFile(String filename) {
        return rootDir.toPath().relativize(Path.of(filename)).toFile();
    }

    protected void printHelpAndExit(boolean onerror) {
        (onerror ? System.err : System.out).println("" +
                "Usage:\n" +
                "aigenpipeline [options] [<input_files>...]\n" +
                "\n" +
                "Options:\n" +
                "  -h, --help               Show this help message and exit.\n" +
                "  --version                Show the version of the AIGenPipeline tool and exit.\n" +
                "  -o, --output <file>      Specify the output file where the generated content will be written. Mandatory.\n" +
                "  -p, --prompt <file>      Reads a prompt from the given file.\n" +
                "  -s, --sysmsg <file>      Optional: Reads a system message from the given file instead of using the default. \n" +
                "  -v, --verbose            Enable verbose output to stderr, providing more details about the process.\n" +
                "  -n, --dry-run            Enable dry-run mode, where the tool will only print to stderr what it would do without \n" +
                "                           actually calling the AI or writing any files.\n" +
                "  -c, --check              Only check if the output needs to be regenerated based on input versions without actually \n" +
                "                           generating it. The exit code is 0 if the output is up to date, 1 if it needs to be \n" +
                "                           regenerated.\n" +
                "  -f, --force              Force regeneration of output files, ignoring any version checks.\n" +
                "  --ask <question>         Asks the AI a question about the generated result. This needs _exactly_the_same_command_line_\n" +
                "                           that was given to generate the output file, and the additional --ask <question> option.\n" +
                "                           It recreates the conversation that lead to the output file and asks the AI for a \n" +
                "                           clarification. The output file is not written, but read to recreate the conversation.\n" +
                "  -u, --url <url>          The URL of the AI server. Default is https://api.openai.com/v1/chat/completions .\n" +
                "                           In the case of OpenAI the API key is expected to be in the environment variable \n" +
                "                           OPENAI_API_KEY, or given as -k option.\n" +
                "  -k, --key <key>          The API key for the AI server. If not given, it's expected to be in the environment variable \n" +
                "                           OPENAI_API_KEY, or you could use a -u option to specify a different server that doesnt need\n" +
                "                           an API key. Used in \"Authorization: Bearer <key>\" header.\n" +
                "  -m, --model <model>      The model to use for the AI. Default is gpt-4-turbo-preview .\n" +
                "  -t <maxtokens>           The maximum number of tokens to generate.\n" +
                "\n" +
                "Arguments:\n" +
                "  [<input_files>...]       Input files to be processed. \n" +
                "\n" +
                "Examples:\n" +
                "  Generate documentation from a prompt file:\n" +
                "    aigenpipeline -p prompts/documentation_prompt.txt -o generated_documentation.md src/foo/bar.java src/foo/baz.java\n" +
                "\n" +
                "  Force regenerate an interface from an OpenAPI document, ignoring version checks:\n" +
                "    aigenpipeline -f -o specs/openapi.yaml -p prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java\n" +
                "\n" +
                "  Ask how to improve a prompt after viewing the initial generation of specs/openapi.yaml:\n" +
                "    aigenpipeline -o PreviousOutput.java -p prompts/promptGenertaion.txt specs/openapi.yaml --ask \"Why did you not use annotations?\"  \n" +
                "\n" +
                "Note:\n" +
                "  It's recommended to manually review and edit generated files. Use version control to manage and track changes over time. \n" +
                "  More detailed instructions and explanations can be found in the README at https://github.com/stoerr/AIGenPipeline .\n"
        );
        System.exit(onerror ? 1 : 0);
    }

    protected void parseArguments(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    help = true;
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
                    force = true;
                    break;
                case "-e":
                case "--explain":
                    explain = args[++i];
                    break;
                case "-u":
                case "--url":
                    url = args[++i];
                    break;
                case "-k":
                case "--key":
                    key = args[++i];
                    break;
                case "-m":
                case "--model":
                    model = args[++i];
                    break;
                case "-t":
                case "--maxtokens":
                    tokens = Integer.parseInt(args[++i]);
                    break;
                default:
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

}
