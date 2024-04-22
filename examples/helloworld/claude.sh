#!/usr/bin/env bash
# call aigenpipeline with Anthropic Claude
# see https://docs.anthropic.com/claude/docs/models-overview
rm -f helloworld.js
AIARGS="-u https://api.anthropic.com/v1/messages -m $ANTHROPIC_DEFAULT_MODEL -a $ANTHROPIC_API_KEY"
aigenpipeline $AIARGS -p helloworld.prompt -o helloworld.js
cat helloworld.js
