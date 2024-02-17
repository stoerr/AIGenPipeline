# Implementation of the AIGenPipeline

## Basic decisions

I'll use Java as implementation basis as I intend to use it in various ways: command line tool, framework for code 
generation in my projects, possibly a maven plugin.

The build will be a maven build, hosted on GitHub and built / distributed with GitHub Actions.
There will be two maven modules: one for the command line tool, and one for the framework. The framework will be used in
the command line tool, and can be used in other projects.

## Maven Coordinates

The maven coordinates will be:
- groupId: `net.stoerr.ai.aigenpipeline`
- artifactId: `aigenpipeline-commandline` for the command line tool, and `aigenpipeline-framework` for the framework.
- version: `0.1-SNAPSHOT` for now.
- packaging: `jar` for both. The command line is a runnable jar.
- name: `AIGenPipeline Command Line Tool` and `AIGenPipeline Framework`
- description: `A command line tool and framework for generating code and documentation with AI.`
- url: `https://aigenpipeline.stoerr.net`
- scm: `https://github.com/stoerr/AIGenPipeline.git`
- Package names: `net.stoerr.ai.aigenpipeline.commandline` and `net.stoerr.ai.aigenpipeline.framework`
- Main class commandline: `net.stoerr.ai.aigenpipeline.commandline.AIGenPipeline`
