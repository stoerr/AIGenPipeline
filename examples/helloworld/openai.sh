#!/usr/bin/env bash
# call aigenpipeline with OpenAI
rm -f helloworld.js
# These arguments are not strictly necessary (= defaults) but for testing:
AIARGS="-u https://api.openai.com/v1/chat/completions -a $OPENAI_API_KEY -m gpt-4o-mini"
aigenpipeline $AIARGS -p helloworld.prompt -o helloworld.js
cat helloworld.js
