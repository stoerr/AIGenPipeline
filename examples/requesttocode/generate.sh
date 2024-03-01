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
$CMD -p codeRules.prompt -p createcode.prompt -o ChatGPTPromptExecutorImpl.java $EXISTINGOUTFILE ChatGPTPromptExecutor.java request.json response.json
