#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
CMD="$CMD -m gpt-4-turbo-preview"
# CMD="$CMD -v"
OUTFILE="ChatGPTPromptExecutorImpl.java"
EXISTINGOUTFILE=""
if [ -f "$OUTFILE" ]; then
  EXISTINGOUTFILE="$OUTFILE"
fi
set -x -e

# Minimize changes if requirements are changed
$CMD -v -p codeRules.prompt -p createcode.prompt -o ChatGPTPromptExecutorImpl.java $EXISTINGOUTFILE ChatGPTPromptExecutor.java request.json response.json

# Or just regenerate
# $CMD -p codeRules.prompt -p createcode.prompt -o ChatGPTPromptExecutorImpl.java ChatGPTPromptExecutor.java request.json response.json
