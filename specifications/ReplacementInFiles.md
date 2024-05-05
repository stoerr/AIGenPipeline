# Specification for extension: replacement of texts in files

## Basic idea

This extension allows to replace parts of files instead of regenerating a whole file. This allows combining a file
directly from manual texts and several generated parts. In contrast to the normal overwriting we need to have a
start and end marker, one of those should contain the versioning comment. The advantage is that we don't need to
understand the content syntax. Since there can be several AI generated fragments in the file, we need to somehow
mark the part.

Additional command line argument:
-r <replacementmarker>   Replace a part of the output file, starting with the first line containing the replacement
marker and stop with the second line containing it.

## Example

```
This is a hand generated part
<!-- Start aipart123 AIGenVersion(xxx) -->
<!-- End aipart123 -->
```

needs calling the tool with `-r aipart123` . There should be a AIGenVersion marker from the very start so that
the tool knows where to place that marker.

## Extension (not implemented yet): both prompt and generated text are in the same file

Parts of the file:

- AIGenVersion tag
- the command line: input file(s), additional prompt files, modus, evtl. -wp,
- prompt
- generated text

The AIGenVersion tag, prompt and generated text can be in one comment block at the start, and the rest of the file the
generated text. Perhaps we can have several generated parts with -wp?
One problem: reformatting with the IDE shouldn't destroy anything.

Basic idea: we put something into a file that says "insert here the output that is generated the following prompt 
based on the following files."

Idea:

/* AIGenVersion(4684af8d, 0dialogelements.prompt-54ed1022, README.md-1.0)
AIGenCmdline(input.txt)
AIGenPromptStart(foo)
bla bla bla
AIGenPromptEnd(foo) */
