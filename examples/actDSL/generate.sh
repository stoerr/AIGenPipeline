#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
# CMD="$CMD -m gpt-3.5-turbo"

# If using Anthropic Claude-3: Haiku doesn't work, but Sonnet and Opus do
# https://docs.anthropic.com/claude/docs/models-overview#model-comparison
# CMD="$CMD -u https://api.anthropic.com/v1/messages -m claude-3-sonnet-20240229"

# CMD="$CMD -v"
set -v -e
$CMD -p actdsl.prompt greeterexample.txt -o greeterexample.js
