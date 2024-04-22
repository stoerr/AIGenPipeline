# Some examples of employing the AIGenPipeline

In this folder you will find a couple of examples of how to use the AIGenPipeline for generating code, transforming 
content, and other tasks. Here it's done with the command line tool, but for complicated tasks involving many files
you could also consider using the framework.

The examples always contain a generate.sh script that triggers the generation process. Feel free to experiment with 
this!

## [helloworld](helloworld/)

Generates "print hello world" in Javascript with OpenAI and Anthropic Claude.

## [Translation and Rewriting](translationAndTransformation/)

For the Composum AI I needed a number of prompts translated into several languages, and transformed into pages for 
the AEM CMS. [generate.sh](translationAndTransformation/generate.sh) does that in two steps: it translates the
original [0predefinedprompts.json](translationAndTransformation/0predefinedprompts.json) into the needed languages
`1prompts-*.json` for easier inspection, and then transforms them into the pages `2page-*.xml`. Thus, an update to 
0predefinedprompts.json retriggers the whole process; if a translation in one of the language-specific files is 
changed, only the transformation of that file is redone. (That needs changing the AIGenVersion in the modified file,
though.)

Caution: running that with GPT-4 takes 10 minutes and costs in the order of 0.25$. With GPT-3.5 it's peanuts and 2
minutes, though a bit lower quality.

## [ACT DSL](actDSL)

That's an interesting idea I picked up at Clemens Helm's interesting talk at
[JUG Saxony Day 2023](https://jugsaxony.org/timeline/2023/9/29/JSD2023) .

For some kinds of repetitive jobs DSLs (domain specific languages) are a very nice productivity tools that can
give you a boost when starting up or even go the whole way. I often did that in Scala since that nicely combines
type safety, code completion and easy access to documentation in the IDE and a concise syntax. But because of LLM
we have a new option in cases where implementing such a DSL would be too much effort. Clemens' nice idea is to define 
the DSL as rules for ChatGPT how to create the code. For the code generation we can then use the AIGenPipeline.

## [Generate Java Code for a REST API from example request](requesttocode/)

This example generates Java sourcec ode to access a simple version of OpenAI's chat completion service just from 
examples for request and responses. It demonstrates several things:

- The prompt and input files can be spread out as needed into various files containing request, response, 
  documentation, prompts.
- A nice way to tell the AI what you expect from the generated code is to give it an interface to implement.
- If you have already generated code or specification and want to update / evolve it from changed specifications, you 
  can try to add the (already existing) output file as first input file to the AI. Not sure whether that always 
  works and it will probably need adapting the instructions sometimes, but as you can try it seems to work nicely here.
