#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
CMD="$CMD -m gpt-3.5-turbo"
# CMD="$CMD -v"
set -v -e
# $CMD -p 0dialogelements.prompt README.md -o dialogelements.txt
$CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html
# $CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html --explain 'why did you not include the Initial value: "Translate this text into " in the instructions field? How should I change the README.md so that it is included?'
$CMD -p 2css.prompt README.md differentialReTranslation.html -o differentialReTranslation.css
$CMD -p 3js.prompt README.md dialogelements.txt requests.jsonl -o differentialReTranslation.js
echo DONE
