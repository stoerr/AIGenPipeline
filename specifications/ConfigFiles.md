# Configuration files

## Basic idea

The tool should be able to read configuration files with common configurations (e.g. which AI backend to use).
These should simply contain command line options; we'll split it at whitespaces just like in bash.
Also there should be an environment variable that can contain options - e.g. reading configurations from a file, or
the options themselves.

The environment variable should be named `AIGENPIPELINE_CONFIG`.

The configuration file(s) to read can be specified from the command line. I'd like it to scan upwards from the 
current directory for files named `.aigenpipeline`. This way we can have a global configuration file in the root
of the project, and a local one in the current directory, or one in the users home directory etc. There should be an 
option to switch that behaviour off, though. If that option is given in one of these configuration files, that 
aborts the scanning further upwards.

The order these configurations are processed is: environment variable, `.aigenpipeline` files from top to bottom,
command line arguments. Thus the later override the earlier one, as these get more specific to the current call.
If a String like $ENVVAR is found in the configuration file, it should be replaced with the value of the environment.

Lines in configuration files starting with # will be ignored (comments).

## Command line arguments

- Read configuration from a file : `-cf <configfile>`, long form `--configfile <configfile>` 
- Do not scan for `.aigenpipeline` config files: `-cn, --confignoscan`
- ignore the environment variable: `-cne, --configignoreenv`
