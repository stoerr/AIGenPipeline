#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
CMD="$CMD -m gpt-3.5-turbo"
# CMD="$CMD -v"
set -v -e
$CMD -p actdsl.prompt greeterexample.txt -o greeterexample.js
