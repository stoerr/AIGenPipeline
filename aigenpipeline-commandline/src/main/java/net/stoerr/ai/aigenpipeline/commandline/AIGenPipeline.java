package net.stoerr.ai.aigenpipeline.commandline;

import java.util.ArrayList;
import java.util.List;

import net.stoerr.ai.aigenpipeline.framework.chat.AIChatBuilder;
import net.stoerr.ai.aigenpipeline.framework.chat.OpenAIChatBuilderImpl;

public class AIGenPipeline {

    private boolean help, verbose, dryRun, check, force;
    private String output, ask, url, key;
    private String extension = "md";
    private List<String> inputFiles = new ArrayList<>();
    private List<String> promptFiles = new ArrayList<>();
    private AIChatBuilder chatBuilder = new OpenAIChatBuilderImpl();

    public static void main(String[] args) {
        new AIGenPipeline().run(args);
    }

    protected void run(String[] args) {
        parseArguments(args);
        if (help) {
            printHelpAndExit(false);
        }
        run();
    }

    protected void printHelpAndExit(boolean onerror) {
        (onerror ? System.err : System.out).println("" +
                "Usage:\n" +
                "aigenpipeline [options] [<input_files>...]\n" +
                "\n" +
                "Options:\n" +
                "  -h, --help               Show this help message and exit.\n" +
                "  -o, --output <file>      Specify the output file where the generated content will be written. stdout if not given.\n" +
                "  -p <prompt_file>         Explicitly declares the file to be a prompt (instruction) file. This is necessary if the \n" +
                "                           file extension is not recognized as a prompt file. \n" +
                "  -i <input_file>          Explicitly declares the file to be an input file (e.g. even if it contains '.prompt').\n" +
                "  -v, --verbose            Enable verbose output to stderr, providing more details about the process.\n" +
                "  -n, --dry-run            Enable dry-run mode, where the tool will only print to stderr what it would do without \n" +
                "                           actually calling the AI or writing any files.\n" +
                "  --version                Show the version of the AIGenPipeline tool.\n" +
                "  -c, --check              Only check if the output needs to be regenerated based on input versions without actually \n" +
                "                           generating it. The exit code is 0 if the output is up to date, 1 if it needs to be \n" +
                "                           regenerated.\n" +
                "  -f, --force              Force regeneration of output files, ignoring any version checks.\n" +
                "  -e extension             Specify the file type as extension (md,java,css,html,xml) used for determining comment \n" +
                "                           syntax to use for version comments in the output file. If -o is given, it's auto detected.\n" +
                "                           Default to put a /* */ comment at the start of the file.\n" +
                "  --ask <question>         Asks the AI a question about the generated result. This needs _exactly_the_same_command_line_\n" +
                "                           that was given to generate the output file, and the additional --ask <question> option.\n" +
                "                           It recreates the conversation that lead to the output file and asks the AI for a \n" +
                "                           clarification. The output file is not written, but read to recreate the conversation.\n" +
                "  -u <url>                 The URL of the AI server. Default is https://api.openai.com/v1/chat/completions .\n" +
                "                           In the case of OpenAI the API key is expected to be in the environment variable \n" +
                "                           OPENAI_API_KEY, or given as -k option.\n" +
                "  -k <key>                 The API key for the AI server. If not given, it's expected to be in the environment variable \n" +
                "                           OPENAI_API_KEY, or you could use a -u option to specify a different server that doesnt need\n" +
                "                           an API key. Used in \"Authorization: Bearer <key>\" header.\n" +
                "\n" +
                "Arguments:\n" +
                "  [<input_files>...]       Optional paths to additional input files to be processed. If it contains '.prompt' it'll be\n" +
                "                           treated as prompt file, otherwise as source file. There has to be at least one prompt file,\n" +
                "                           possibly declared with -p if it hasn't .prompt extension (or e.g. .prompt.txt or \n" +
                "                           .prompt.md). \n" +
                "\n" +
                "Examples:\n" +
                "  Generate documentation from a prompt file:\n" +
                "    aigenpipeline prompts/documentation_prompt.txt -o generated_documentation.md src/foo/bar.java src/foo/baz.java\n" +
                "\n" +
                "  Force regenerate an interface from an OpenAPI document, ignoring version checks:\n" +
                "    aigenpipeline -f -o specs/openapi.yaml prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java\n" +
                "\n" +
                "  Ask how to improve a prompt after viewing the initial generation:\n" +
                "    aigenpipeline -o PreviousOutput.java prompts/promptGenertaion.txt specs/openapi.yaml --ask \"Why did you not use annotations?\"  \n" +
                "\n" +
                "Note:\n" +
                "  It's recommended to manually review and edit generated files. Use version control to manage and track changes over time. \n" +
                "  More detailed instructions and explanations can be found in the README at https://github.com/stoerr/AIGenPipeline .\n"
        );
        System.exit(onerror ? 1 : 0);
    }

    protected void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    help = true;
                    break;
                case "-o":
                case "--output":
                    output = args[++i];
                    break;
                case "-p":
                    promptFiles.add(args[++i]);
                    break;
                case "-i":
                    inputFiles.add(args[++i]);
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
                    extension = args[++i];
                    break;
                case "--ask":
                    ask = args[++i];
                    break;
                case "-u":
                    url = args[++i];
                    break;
                case "-k":
                    key = args[++i];
                    break;
                default:
                    // Treat as input file if not a known option
                    inputFiles.add(args[i]);
                    break;
            }
        }
    }

    protected void run() {
        // ChatGPTTask: implement this.
    }

}
