# Plugin Action: executes the command line tool with optionally given command line arguments

if [ -z "$1" ]; then
  additionalArgs=$(cat)
else
  additionalArgs="$@"
fi

jarFile=$(ls -1 aigenpipeline-commandline/target/aigenpipeline-commandline*.jar | tail -n 1)

if [ -z "$jarFile" ]; then
  echo "No jar file found in aigenpipeline-framework/target"
  exit 1
fi

echo executing java -jar $jarFile $additionalArgs
exec java -jar $jarFile $additionalArgs
