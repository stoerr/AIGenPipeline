# Todos for the application

- Possibility to avoid AI FIXME
- On exception display the command line to reproduce

- Add quoting to the command line parsing for option files and infile prompting command line
- implement continuing a file on length limit
- make a video

## Ideas for command line / framework improvements

- prompt on command line? stdin? unclear what filename!
    - (idea: -p - = stdin , versioning from tag)
- improve automated deployment with GitHub Actions ; automatically push to master

## Site improvements

- better document usage of various backends
- split into several pages
- examples
    - json prompts to page
    - Request example to GSON class
    - construct the readmes of the examples?
    - HTL to md to java?
    - construct mini-application?
    - demonstrate explain feature

## Possible other endpoints to check

https://octo.ai/product/text-generation/
Huggingface?
Claude, Gemini
https://console.groq.com/docs/openai - https://simonwillison.net/2024/Apr/22/llama-3/
https://openrouter.ai/models/meta-llama/llama-3-70b-instruct

llm? llm -m Meta-Llama-3-8B-Instruct
llm chat -m Meta-Llama-3-8B-Instruct

## Maven central publishing links

- https://github.com/volkodavs/ossrh-demo
- https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven
- https://www.baeldung.com/maven-repo-github
- https://stackoverflow.com/questions/14013644/hosting-a-maven-repository-on-github
- https://central.sonatype.org/publish/publish-portal-maven/#introduction
- https://github.com/cometbid-sfi/test-gitaction-workflow
- https://github.com/actions/setup-java
