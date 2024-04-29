---
description: How to use other LLM with the AI Generation Pipeline
---

# Using various large language models (LLM)

While the AIGenPipeline uses the OpenAI chat completion API as default, it's easy to configure it to use different
models. Here is a description how to configure OpenAI, [Anthropic Claude](https://www.anthropic.com/claude)
[text generation](https://docs.anthropic.com/claude/docs/text-generation) and local models e.g. run
with [LM Studio](https://lmstudio.ai/). Many similar services use an API that is similar or even mostly compatible
to OpenAI's chat completion interface, so you can probably use them with few or no changes in the tool - please
contact me if you need something else, or make a PR.

Configuring the model needs setting some command line options. If you don't want to give them in every call of the
command line tool, you can either put them into the `AIGENPIPELINE_CONFIG` environment variable, or create a
`.aigenpipeline` file that contains them. The tool searches from the current directory upwards, so this can be done
in the project directory where you run the tool or even into your home directory.

The directory [examples/variousModels](https://github.com/stoerr/AIGenPipeline/tree/develop/examples/variousModels) 
contains several examples.

## OpenAI chat completion API

This is the default, so you don't need to specify the API key. If you just put it into an environment variable
`OPENAI_API_KEY` you don't need any special options. Otherwise, you could create a `.aigenpipeline` file like this:

    # for completeness you could give the URL, but that URL hasn't to be set since it's the default.
    # -u https://api.openai.com/v1/chat/completions
    -a $OPENAI_API_KEY
    # optional, if you need that
    -org $OPENAI_ORG_ID
    -m gpt-3.5-turbo 

    # Don't look for configuration files in our parent directories
    -cn

This references environment variables, but you can also put the actual keys into that file.

## Anthropic claude

The [Anthropic Claude](https://www.anthropic.com/claude)
[text generation](https://docs.anthropic.com/claude/docs/text-generation) service needs the following arguments in
a `.aigenpipeline` file. If the key is in an environment variable `ANTHROPIC_API_VERSION`, the -a line with the key
can be omitted.

    -u https://api.anthropic.com/v1/messages
    -m $ANTHROPIC_DEFAULT_MODEL
    -a $ANTHROPIC_API_KEY

    # Don't look for further configuration files
    -cn

This references environment variables, but you can also put the actual keys into that file.

## Local models

For [LM Studio](https://lmstudio.ai/) the configuration file `.aigenpipeline` can look like this:

    # for completeness, but that URL hasn't to be set since it's the default.
    -u http://localhost:1234/v1/chat/completions
    
    # Don't look for further configuration files
    -cn

Or you could put `-u http://localhost:1234/v1/chat/completions` into `AIGENPIPELINE_CONFIG` if you always use that.
