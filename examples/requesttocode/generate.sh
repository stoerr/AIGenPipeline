#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
CMD="$CMD -m gpt-3.5-turbo"
# CMD="$CMD -v"
set -v -e
$CMD -p codeRules.prompt -p createcode.prompt -o ChatGPTPromptExecutorImpl.java ChatGPTPromptExecutor.java request.json response.json
