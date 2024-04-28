#!/usr/bin/env bash
aigenpipeline -cne -wo -s whatareyou.prompt -o answer.txt
cat answer.txt
