#!/usr/bin/env bash
aigenpipeline -cne -wo -ga -s emptysysmsg.txt -p whatareyou.prompt -o answer.txt
cat answer.txt
