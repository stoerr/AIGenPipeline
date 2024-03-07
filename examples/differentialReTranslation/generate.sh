#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
# CMD="$CMD -v"
# CMD="$CMD -n" # dry run
CMD="$CMD -m gpt-3.5-turbo"
CMD4="$CMD -m gpt-4-turbo-preview"
set -v -e
$CMD -p 0dialogelements.prompt README.md -o dialogelements.txt
$CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html
# example for check: $CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html --explain 'why did you not include the Initial value: "Translate this text into " in the instructions field? How should I change the README.md so that it is included?'
$CMD -p 2css.prompt README.md differentialReTranslation.html -o differentialReTranslation.css
$CMD4 -p 3js.prompt README.md dialogelements.txt requests.jsonl -o differentialReTranslation.js
$CMD -p 4examplejs.prompt README.md dialogelements.txt examples.txt -o differentialReTranslationExamples.js
echo DONE
