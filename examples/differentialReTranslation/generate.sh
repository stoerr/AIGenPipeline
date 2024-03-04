#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
CMD="$CMD -m gpt-3.5-turbo"
# CMD="$CMD -v"
set -v -e
$CMD -p 0dialogelements.prompt README.md -o dialogelements.txt
$CMD -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html
