#!/usr/bin/env bash
srcdir=../..
# srcdir=/Users/hps/dev/composum/composum-chatgpt-integration
jsondir=descriptions

if [ ! -f CodeDuplicationFinder.md ]; then
  echo "CodeDuplicationFinder.md not found, we're in the wrong directory. Exiting."
  exit 1
fi
mkdir -p $jsondir

set -x

# Calculate the descriptions for the Java source files as JSON files
for fil in $(find $srcdir -name '*.java' | fgrep src/main/java); do
  filrelativepath=$(realpath --relative-to=$srcdir $fil)
  jsonfile=$jsondir/$filrelativepath.json
  mkdir -p $(dirname $jsonfile)

  # Either generate a description
  aigenpipeline -s sysprompt.prompt -p extractdescriptions.prompt -o $jsonfile $fil
  # Or extract the source code
  # aigenpipeline -s sysprompt.prompt -p extractsource.prompt -o $jsonfile $fil

  rm -f $jsondir/$filrelativepath*.descr
  node extractMethodsFromJson.js $jsonfile
done

db=$(realpath db/embeddings.db)
(
  cd $jsondir
  llm embed-multi descr -d $db -m 3-large --store --files . '**/*.descr'
)
# search most similar lines can be done e.g. with llm similar descr -d db/embeddings.db -n 5 -c "argument"

# write it as json
sqlite-utils rows db/embeddings.db embeddings --nl -c id -c content -c embedding > db/embeddings.jsonl

./findClosest.js > result.txt

echo The closest found duplicates are in result.txt
