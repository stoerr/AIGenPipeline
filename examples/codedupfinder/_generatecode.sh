#!/usr/bin/env bash
if [ ! -f CodeDuplicationFinder.md ]; then
  echo "CodeDuplicationFinder.md not found, we're in the wrong directory. Exiting."
  exit 1
fi
aigenpipeline -f -v -os '*'
