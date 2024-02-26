# Some examples of employing the AIGenPipeline

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
