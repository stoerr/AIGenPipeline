# Command line tool AIGenPipeline

## Basic idea

This is a command line tool to generate files using an AI - either ChatGPT or a model with a similar chat completion
interface. It can be used to generate code, documentation, or other text files. Each run of the command line tool takes
at least one prompt file with instructions what to generate as argument, but can also take other source files to be
processed as further input. The output is written to a text file.

Of course it can be used to solve complex tasks with several steps by chaining several runs of the tool -
e.g. create an OpenAPI document from a specification, then generate an interface from
that, a test and an implementation class. Of course, manual inspection and editing of the generated files will usually
be necessary.

I suggest to check in the intermediate and final results into a version control system like Git. That ensures manual
checks when they are regenerated, and minimizes regeneration.

## Caching and versioning

Since generation takes time, costs a little and the results have to be manually checked, the tool takes precaution
not to regenerate the output files unless the input changes.

In the simplest case it can just do nothing when the output file is already there. If input files are changed,
the output file would have to be removed to enforce regeneration.

Another work mode is checking whether the output file is newer than the input files (e.g. Makefile style usage)
But this has the problem that that does not sensibly work if the output files are checked in with a version control
system with Git, and would also regenerate files in case of minor changes in the input files.

The main suggested work mode (which is also the default) is to provide the input and prompt files with version comments
that declare the version of the input file, and only generate the output file if it is not present, or regenerate the
output file if the versions have changed. A version comment in the output file will be generated, which declares a
version for the output file (a hash of it's content) and the versions of the used input files. The version comment
in input and prompt files can be manually changed to force regeneration of the output file.

### Structure of version comments

A version comment can e.g. look like this:

    /* AIGenVersion(ourversion, inputfile1@version1, inputfile2@version2, ...) */

To declare a version for a manually created prompt / source file you can just put in a version comment like
`AIGenVersion(1.0)` and change the version number each time you want to force regeneration.

The comment syntax (in this case /* */) is ignored - we just look for the AIGenVersion via regular expression.
A version comment will be written at the start or end of the output file; that and the used comment syntax is
determined by the file extension.

## Other features

If you are not satisfied with the result, the tool can also be used to ask the AI for clarification: ask a question
about the result or have it make suggestions how to improve the prompt. This mode will not write the output file, but
recreate the conversation that lead to the output file and ask the AI for a clarification or suggestion in form of a
chat continuation.

## Limitations and additional ideas (not implemented yet)

- This means the resulting file is always completely regenerated or not at all. How to make differential changes? One
  idea would be to give the previous file to the AI and ask it to make minimal changes.

## Command Usage

```
aigenpipeline [options] [<input_files>...]

Options:
  -h, --help               Show this help message and exit.
  -o, --output <file>      Specify the output file where the generated content will be written. stdout if not given.
  -p <prompt_file>         Explicitly declares the file to be a prompt (instruction) file. This is necessary if the 
                           file extension is not recognized as a prompt file. 
  -i <input_file>          Explicitly declares the file to be an input file (e.g. even if it contains '.prompt').
  -v, --verbose            Enable verbose output to stderr, providing more details about the process.
  -n, --dry-run            Enable dry-run mode, where the tool will only print to stderr what it would do without 
                           actually calling the AI or writing any files.
  --version                Show the version of the AIGenPipeline tool.
  -c, --check              Only check if the output needs to be regenerated based on input versions without actually 
                           generating it. The exit code is 0 if the output is up to date, 1 if it needs to be 
                           regenerated.
  -f, --force              Force regeneration of output files, ignoring any version checks.
  -e extension             Specify the file type as extension (md,java,css,html,xml) used for determining comment 
                           syntax to use for version comments in the output file. If -o is given, it's auto detected.
                           Default to put a /* */ comment at the start of the file.
  --ask <question>         Asks the AI a question about the generated result. This needs _exactly_the_same_command_line_
                           that was given to generate the output file, and the additional --ask <question> option.
                           It recreates the conversation that lead to the output file and asks the AI for a 
                           clarification. The output file is not written, but read to recreate the conversation.
  -u <url>                 The URL of the AI server. Default is https://api.openai.com/v1/chat/completions .
                           In the case of OpenAI the API key is expected to be in the environment variable 
                           OPENAI_API_KEY, or given as -k option.
  -k <key>                 The API key for the AI server. If not given, it's expected to be in the environment variable 
                           OPENAI_API_KEY, or you could use a -u option to specify a different server that doesnt need
                           an API key. Used in "Authorization: Bearer <key>" header.

Arguments:
  [<input_files>...]       Optional paths to additional input files to be processed. If it contains '.prompt' it'll be
                           treated as prompt file, otherwise as source file. There has to be at least one prompt file,
                           possibly declared with -p if it hasn't .prompt extension (or e.g. .prompt.txt or 
                           .prompt.md). 

Examples:
  Generate documentation from a prompt file:
    aigenpipeline prompts/documentation_prompt.txt -o generated_documentation.md src/foo/bar.java src/foo/baz.java

  Force regenerate an interface from an OpenAPI document, ignoring version checks:
    aigenpipeline -f -o specs/openapi.yaml prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java

  Ask how to improve a prompt after viewing the initial generation:
    aigenpipeline -o PreviousOutput.java prompts/promptGenertaion.txt specs/openapi.yaml --ask "Why did you not use annotations?"  

Note:
  It's recommended to manually review and edit generated files. Use version control to manage and track changes over time.
```

## Example Usages

1. **Basic Generation**: Generating an OpenAPI document interface, tests, and implementation class could be achieved by
   chaining runs of `AIGenPipeline`, each with the appropriate prompt and input files. For example:

   ```shell
   aigenpipeline -p prompts/openapi_prompt.txt -o generated_openapi.yaml
   aigenpipeline -p prompts/interface_prompt.txt -i generated_openapi.yaml -o generated_interface.java
   aigenpipeline -p prompts/test_prompt.txt -i generated_interface.java -o generated_test.java
   ```

2. **Explanation / Query**: After generating an output, if there are questions or the need for clarification on how to
   improve it:

   ```shell
   aigenpipeline -p prompts/interface_prompt.txt -i generated_openapi.yaml -o generated_interface.java --ask \
        "Why wasn't a @GET annotation used in method foo? How would I have to change the prompt to make sure it's used?"
   ```

3. **Force Regeneration**: If a developer makes significant changes to the input or prompt files and wants to ensure the
   output is regenerated, despite previous versions:

   ```shell
   aigenpipeline -f -o specs/openapi.yaml prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java
   ```

   Alternatively, the output files could just be removed before running the tool, or the version comments in suitable
   input files or prompt file(s) could be changed.
