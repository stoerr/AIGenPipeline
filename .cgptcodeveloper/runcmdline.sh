# Plugin Action: executes the command line tool with optionally given command line arguments

if [ -z "$1" ]; then
  exec bin/aigenpipeline $(cat)
else
  exec bin/aigenpipeline "$@"
fi
