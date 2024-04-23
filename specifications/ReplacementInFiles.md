# Specification for extension: replacement of texts in files

## Basic idea

This extension allows to replace parts of files instead of regenerating a whole file. This allows combining a file
directly from manual texts and several generated parts. In contrast to the normal overwriting we need to have a
start and end marker, one of those should contain the versioning comment. The advantage is that we don't need to
understand the content syntax. Since there can be several AI generated fragments in the file, we need to somehow
mark the part.

Additional command line argument:
-r <replacementmarker>   Replace a part of the output file, starting with the first line containing the replacement
marker and stop with the second line containing it.

## Example

```
This is a hand generated part
<!-- Start aipart123 AIGenVersion(xxx) -->
<!-- End aipart123 -->
```

needs calling the tool with `-r aipart123` . There should be a AIGenVersion marker from the very start so that
the tool knows where to place that marker.
