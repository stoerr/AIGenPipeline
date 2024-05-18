# Todos for the application

- Bug: when using -os and giving the output file as input to update then we should not give the whole file but the 
  part. Or do we? Maybe give both, or an additional switch to update.
- for updating: possibly give additional instructions for focusing on specific changes that is not included into the
  normal prompt file and versioning. (<-> prompt on command line? And how to do that for -os?)
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
