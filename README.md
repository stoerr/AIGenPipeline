# AI based code generation pipeline

<strong>A command line tool and framework for systematic code generation using AI</strong>

> In silence, code weaves,<br/>
> Through prompts, AI breathes life anew,<br/>
> Scripts bloom, knowledge leaves.
>
> Git guards every step,<br/>
> In the dance of creation,<br/>
> Change blooms, watched and kept.<br/>
> -- ChatGPT

## Basic idea

This is a command line tool to generate files using an AI - either ChatGPT or a model with a similar chat completion
interface. It can be used to generate code, documentation, or other text files. Each run of the command line tool can
take several prompt files with instructions what to generate as argument, but can also take other source files to be
processed as further input. The output is written to a text file.

It can be used to solve complex tasks with several steps by chaining several runs of the tool -
e.g. create an OpenAPI document from a specification, then generate an interface from
that, a test and an implementation class. Of course, manual inspection and editing of the generated files will usually
be necessary.

I suggest to inspect the intermediate and final results and committing them into a version control system like Git.
That ensures manual checks when they are regenerated, and minimizes regeneration.

## Some example usages

1. **Basic Generation**: Generating an OpenAPI document interface, tests, and implementation class could be achieved by
   chaining runs of `AIGenPipeline`, each with the appropriate prompt and input files. For example:

   ```shell
   aigenpipeline -p openapi_prompt.txt -o generated_openapi.yaml
   aigenpipeline -p interface_prompt.txt -i generated_openapi.yaml -o generated_interface.java
   aigenpipeline -p test_prompt.txt -i generated_interface.java -o generated_test.java
   ```

   By the way: if you want to change an existing output file, you can give it as additional input file to the AI. 
   It will be instructed to check and update the file according to the new input files, minimizing the changes.

2. **Explanation / Query**: After generating an output, if there are questions or the need for clarification on how to
   improve it:

   ```shell
   aigenpipeline -p interface_prompt.txt -i generated_openapi.yaml -o generated_interface.java --explain \
        "Why wasn't a @GET annotation used in method foo? How would I have to change the prompt to make sure it's 
   used?"
   ```

3. **Force Regeneration**: Normally a versioning mechanism (see below) ensures the result is not regenerated unless the
   input changes. The `-f` flag disables this checking:

   ```shell
   aigenpipeline -f -o specs/openapi.yaml api_interface_prompt.txt src/main/java/foo/MyInterface.java
   ```

   Alternatively, the output files could just be removed before running the tool, or the version comments in suitable
   input files or prompt file(s) could be changed.

4. **Generate parts of a file**: If you want to combine manually written and ai generated parts in one file, you can use
   the `-wp <marker>` option to replace a part of the output file. The marker should occur in exactly two lines of the
   already existing output file - the lines between them are replaced by the AI generated text. The first line must 
   also contain the version commment (see below).

   ```shell
   aigenpipeline -wp generatedtable -o outputfile.md -p maketablefromjson.txt input.json
   ```
  
   could e.g. replace the part between the lines `<!-- start generatedtable AIGenVersion(1.0) -->` and `<!-- end 
   generatedtable -->` in:

   ```
   This is the hand generated part
   <!-- start generatedtable AIGenVersion(1.0) -->
   | Column1 | Column2 |
   |---------|---------|
   | value1  | value2  |
   <!-- end generatedtable -->
   Here can be more handwritten stuff that is untouched.
   ```

## Caching and versioning

Since generation takes time, costs a little and the results often have to be manually checked, the tool takes precaution
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

## Using different large language models

While the tool defaults to using the OpenAI chat completion service, it is possible to use other services / LLM as 
well. I tried with [Anthropic Claude](https://www.anthropic.com/claude) 
[text generation](https://docs.anthropic.com/claude/docs/text-generation) and some local models run with the nice 
[LM Studio](https://lmstudio.ai/). See [using other models](https://aigenpipeline.stoerr.net/otherModels.md) for 
some examples.

## Configuration files

The tool can read configuration files with common configurations (e.g. which AI backend to use). These should simply
contain command line options; we'll split it at whitespaces just like in bash. Also, there can be an environment 
variable AIGENPIPELINE_CONFIG that can contain options.

Configuration files can be given explicitly (option `-cf` / `--configfile`) or the tool can scan for files named
`.aigenpipeline` upwards from the output file directory. The search for `.aigenpipeline` files can be switched off
with the `-cn` / `--confignoscan` option. If that option is given in one of these configuration files, that aborts
the scanning further upwards in the directory tree.

The order these configurations are processed is: environment variable, `.aigenpipeline` files from top to bottom,
command line arguments. Thus, the later override the earlier one. Explicitly given configuration files are
processed at the point where the argument occurs when processing the command line arguments.

## Other features

If you are not satisfied with the result, the tool can also be used to ask the AI for clarification: ask a question
about the result or have it make suggestions how to improve the prompt. This mode will not write the output file, but
recreate the conversation that lead to the output file and ask the AI for a clarification or suggestion in form of a
chat continuation.

It's also possible to ask the tool questions about itself - use the `-ha` / `--helpai` option with a question, and it 
tries to answer that from this documentation. There is also an 
[OpenAI GPT](https://chatgpt.com/g/g-zheGoARkR-ai-based-code-generation-pipeline-helper)
that can be asked.

## Limitations and additional ideas (not implemented yet)

- This means the resulting file is always completely regenerated or not at all. How to make differential changes? One
  idea would be to give the previous file to the AI and ask it to make minimal changes.

## Command Usage

```
Usage:
aigenpipeline [options] [<input_files>...]

Options:

  General options:
    -h, --help               Show this help message and exit.
    -ha, --helpai <question> Answer a question about the tool from the text on its documentation site and exit.
    --version                Show the version of the AIGenPipeline tool and exit.
    -c, --check              Only check if the output needs to be regenerated based on input versions without actually 
                             generating it. The exit code is 0 if the output is up to date, 1 if it needs to be 
                             regenerated.
    -n, --dry-run            Enable dry-run mode, where the tool will only print to stderr what it would do without 
                             actually calling the AI or writing any files.
    -v, --verbose            Enable verbose output to stderr, providing more details about the process.

  Input / outputs:
    -o, --output <file>      Specify the output file where the generated content will be written. Mandatory.
    -p, --prompt <file>      Reads a prompt from the given file.
    -s, --sysmsg <file>      Optional: Reads a system message from the given file instead of using the default. 
    -k <key>=<value>         Sets a key-value pair replacing ${key} in prompt files with the value. 

  AI Generation control:
    -f, --force              Force regeneration of output files, ignoring any version checks - same as -ga.
    -ga, --gen-always        Generate the output file always, ignoring version checks.
    -gn, --gen-notexists     Generate the output file only if it does not exist.
    -go, --gen-older         Generate the output file if it does not exist or is older than any of the input files.
    -gv, --gen-versioncheck  Generate the output file if the version of the input files has changed. (Default.)
    -wv, --write-version     Write the output file with a version comment. (Default.)
    -wo, --write-noversion   Write the output file without a version comment.
    -wp, --write-part <marker> Replace the lines between the first occurrence of the marker and the second occurrence.                             If a version marker is written, it has to be in the first of those lines and is changed there.                             It is an error if the marker does not occur exactly twice; the output file has to exist.
    -e, --explain <question> Asks the AI a question about the generated result. This needs _exactly_the_same_command_line_
                             that was given to generate the output file, and the additional --explain <question> option.
                             It recreates the conversation that lead to the output file and asks the AI for a 
                             clarification. The output file is not written, but read to recreate the conversation.

  Configuration files:
    -cf, --configfile <file> Read configuration from the given file. These contain options like on the command line.
    -cn, --confignoscan      Do not scan for `.aigenpipeline` config files.
    -cne, --configignoreenv  Ignore the environment variable `AIGENPIPELINE_CONFIG`.

  AI backend settings:

    -u, --url <url>          The URL of the AI server. Default is https://api.openai.com/v1/chat/completions .
                             In the case of OpenAI the API key is expected to be in the environment variable 
                             OPENAI_API_KEY, or given with the -a option.
    -a, --api-key <key>      The API key for the AI server. If not given, it's expected to be in the environment variable 
                             OPENAI_API_KEY, or you could use a -u option to specify a different server that doesnt need
                             an API key. Used in "Authorization: Bearer <key>" header.
    -org, --organization <id> The optional organization id in case of the OpenAI server.
    -m, --model <model>      The model to use for the AI. Default is gpt-4-turbo-preview .
    -t <maxtokens>           The maximum number of tokens to generate.

Arguments:
  [<input_files>...]       Input files to be processed into the output file. 

Examples:
  Generate documentation from a prompt file:
    aigenpipeline -p prompts/documentation_prompt.txt -o generated_documentation.md src/foo/bar.java src/foo/baz.java

  Force regenerate an interface from an OpenAPI document, ignoring version checks:
    aigenpipeline -f -o specs/openapi.yaml -p prompts/api_interface_prompt.txt src/main/java/foo/MyInterface.java

  Ask how to improve a prompt after viewing the initial generation of specs/openapi.yaml:
    aigenpipeline -o PreviousOutput.java -p prompts/promptGenertaion.txt specs/openapi.yaml --explain "Why did you not use annotations?"  

Configuration files:
  These contain options like on the command line. The environment variable `AIGENPIPELINE_CONFIG` can contain options.
  If -cn is not given, the tool scans for files named .aigenpipeline upwards from the output file directory.
  The order these configurations are processed is: environment variable, .aigenpipeline files from top to bottom,
  command line arguments. Thus the later override the earlier one, as these get more specific to the current call.
  Lines starting with a # are ignored in configuration files (comments).

Note:
  It's recommended to manually review and edit generated files. Use version control to manage and track changes over time. 
  More detailed instructions and explanations can be found at https://aigenpipeline.stoerr.net/ .
```
