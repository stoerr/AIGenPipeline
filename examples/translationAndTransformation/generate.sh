#!/usr/bin/env bash
CMD="../../bin/aigenpipeline"
# CMD="$CMD -v"
set -v -e
for lang in de es fr; do
  $CMD -t 2048 0predefinedprompts.json -p 0translate.prompt -o 1prompts-${lang}.json -k "lang=$lang"
  $CMD -t 4096 1prompts-${lang}.json -p 1transformToPage.prompt -o 2page-${lang}.xml
done
