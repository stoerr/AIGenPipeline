#!/usr/bin/env bash
CMDRAW="../../bin/aigenpipeline"
# CMDRAW="$CMDRAW -v"
# CMDRAW="$CMDRAW -n" # dry run
CMD="$CMDRAW -m gpt-3.5-turbo"
CMD4="$CMDRAW -m gpt-4o"

# If using Anthropic Claude-3: Haiku doesn't work, but Sonnet and Opus do
# https://docs.anthropic.com/claude/docs/models-overview#model-comparison
#CMD="$CMDRAW -u https://api.anthropic.com/v1/messages -m claude-3-sonnet-20240229"
#CMD4="$CMDRAW -u https://api.anthropic.com/v1/messages -m claude-3-opus-20240229"

# Alternatively run it with LM Studio, e.g. with Codellama instruct 13B Q6 on a Mac M1
# The result doesn't really work, though. :-(  Drop me a line if you find a model that does!
# CMD="$CMD -u http://localhost:1234/v1/chat/completions"
# CMD4=$CMD

set -x -e

$CMD -p 0dialogelements.prompt README.md -o dialogelements.txt
$CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html
# example for check: $CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html --explain 'why did you not include the Initial value: "Translate this text into " in the instructions field? How should I change the README.md so that it is included?'
$CMD -p 2css.prompt README.md differentialReTranslation.html -o differentialReTranslation.css
$CMD4 -p 3js.prompt README.md dialogelements.txt requests.jsonl -o differentialReTranslation.js
$CMD -p 4examplejs.prompt README.md dialogelements.txt examples.txt -o differentialReTranslationExamples.js
echo DONE
