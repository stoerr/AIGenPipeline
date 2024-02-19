#!/usr/bin/env bash
DIR=aigenpipeline-commandline/src/test/resources
set -vx
bin/aigenpipeline -v -n -o target/testing/3haiku.txt -p $DIR/prompt.txt $DIR/input1.txt $DIR/input2.md $DIR/input3.txt
bin/aigenpipeline -o target/testing/3haiku.txt -p $DIR/prompt.txt $DIR/input1.txt $DIR/input2.md $DIR/input3.txt

diff -u target/testing/3haiku.txt $DIR/expected/3haiku.txt || exit 1
