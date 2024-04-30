#!/usr/bin/env bash
aigenpipeline -cne -wo -ga -s whatareyou.prompt -o answer.txt
cat answer.txt
