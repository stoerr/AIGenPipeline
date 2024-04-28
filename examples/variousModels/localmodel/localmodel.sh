#!/usr/bin/env bash
aigenpipeline -cne -wo -f -p whatareyou.prompt -o answer.txt
cat answer.txt
