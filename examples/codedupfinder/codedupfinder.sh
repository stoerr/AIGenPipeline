#!/usr/bin/env bash
srcdir=../..
jsondir=1descriptionjson

if [ ! -f CodeDuplicationFinder.md ]; then
  echo "CodeDuplicationFinder.md not found, we're in the wrong directory. Exiting."
  exit 1
fi
mkdir -p $jsondir

# find the java source files
for fil in $(find $srcdir -name '*.java' | fgrep src/main/java); do
  filrelativepath=$(realpath --relative-to=$srcdir $fil)
  jsonfile=$jsondir/$filrelativepath.json
  mkdir -p $(dirname $jsonfile)
  aigenpipeline -p 1extractdescriptions.prompt -o $jsonfile $fil
  cat $jsonfile
  rm -f $jsonfile.*.descr
  node extractMethodsFromJson.js $jsonfile
  exit
done
