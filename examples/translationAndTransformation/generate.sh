#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
# CMD="$CMD -v"
set -v -e
$CMD -t 2048 0predefinedprompts.json -p 0translate.prompt -o 1germanprompts.json
$CMD -t 4096 1germanprompts.json -p 1transformToPage.prompt -o 2page.xml
