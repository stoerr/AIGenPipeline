#!/usr/bin/env bash
echo "This generates the knowledge file to the GPT that answers questions about the tool."
# check that in the current directory there is a .git file - otherwise we are not in the root of the project
if [ ! -d .git ]; then
  echo "This script must be run from the root of the project."
  exit 1
fi

# we collect src/site/markdown/*.md into target/gptknowledge.txt
mkdir -p target
OUT=target/gptknowledge.txt
/bin/rm -f $OUT
for fil in src/site/markdown/*.md; do
  # write ================ CONTENT OF https://aigenpipeline.stoerr.net/(filename with .html instead of .md) ================ into the file
  url=https://aigenpipeline.stoerr.net/$(basename $fil .md).html
  # if url ends with index.html, we remove it
  url=${url%/index.html}
  echo "=============== CONTENT OF $url ================" >> $OUT
  cat $fil >> $OUT
  echo "" >> $OUT
done

echo "Generated $OUT"
