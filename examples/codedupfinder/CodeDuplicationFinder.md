For today's let's have some fun with AI I'd like to spend a few hours to see how far we get with our AI toolbox
to implement a nice interesting idea as a proof of concept. I've recently heard an interesting talk about some
experiments how to use AI techniques to try to find code duplicates. Recalling from memory, these were about the
steps taken:

1. extract all methods from the source code including information about the types used in that method
2. use an LLM to summarize what the method does
3. use an embedding on all summaries to transform that into vector space
4. find the closest matches
5. use an LLM to rate how close those closest matches really are
6. present the best results

Now let's see how to implement that the simplest way, using LLM utilities and Unix command line utilities.

## Preconditions

I'll use a couple of tools I had installed anyway, so if you want to run this yourself you have to install them, too.
Also I'll use OpenAI APIs, so you need an API key, though it isn't difficult to run this with local models, too. 
(Please ask me if you like to do that and I'll extend this document with the necessary steps.)

## Step 1 and 2: extract the methods

In the original talk they used an actual parser for the language to collect features from the code. That's way more 
than a few hours, so we try to use a LLM for that, too. So we want to feed all classes to the LLM and get a couple 
of documents out of it, one for each method. Since we are interested in the descriptions of the methods, we rather 
fuse step 1 and 2 into one step and have the LLM output summaries in the first place. To get one document per method 
let's have JSON output as an array of descriptions, and split that with unix utilities.

There is obvious room for improvement here: for instance it'd be nice for a class implementing an interface to feed 
that interface as background information to the LLM, too, but let's keep it simple for now.

That's a nice job for the AIGenPipeline. We need to create a prompt that reads the source code and outputs the JSON
array of method descriptions. AIGenPipeline will also ensure that the output is regenerated if and only if the 
source file changes.

To split the JSON array into separate documents it's probably easiest to make nodejs javascript.
[extractMethodsFromJson.js](extractMethodsFromJson.js) does that. By the way: the Javascript was generated by
ChatGPT-4 using aigenpipeline from the initial comment in this file: it describes the task and the aigenpipeline
scans from script [_generatecode.sh](_generatecode.sh) where such prompts need to be executed.

## Step 3 - calculate embeddings

The [LLM](https://github.com/simonw/llm) tool by [Simon Willison](https://simonwillison.net/) offers many LLM 
related features, e.g. 
[calculating embeddings for sets of files](https://til.stoerr.net/LLM/llm-embeddings-swillison.html),
recalculating them only when needed. We can calculate the embeddings and save them in a SQLite database with the 
following command:

    llm embed-multi descr -d embeddings.db -m 3-large --store --files "$jsondir" '**/*.descr

The nice thing is that LLM offers a simple similarity search that could even use this to find relevant methods for 
something, though output is as a JSON format (jq is only for readable formatting):

    llm similar descr -d embeddings.db -n 10 -c "description to search for" | jq

## Step 4 - find the closest matches and present them

While llm offers a similarity search, we need to get all of the embeddings and compare them. That's probably best 
done with a program. I'll go for exporting the database to JSON and read it into a Javascript program for nodejs, 
since that needs the least amount of things to learn for this quick project. :-) This exports it:

    sqlite-utils rows embeddings.db embeddings --nl -c id -c content -c embedding > embeddings.json

For now we'll skip the step 5 (rating the matches with an LLM) and just present the best matches, since in the talk
they thought that didn't work too well, anyway.

## Step 5 - rating the matches

Here aigenpipeline again comes in handy. [evaluateDuplicates.js](evaluateDuplicates.js) reads out the results of 
step 4, and has aigenpipeline create a file that has GPT-4o rate the matches. Before printing an estimated number of 
lines that will be saved when deduplicating, we have it print a reason why it thinks it's a duplicate and a little 
plan how to deduplicate it - that might be interesting for the developer, and also improve the result in the style of
"chain of thought": having the LLM generate an explanation will likely improve the result in comparison to 
just having it rate the matches. The prompt 
[evaluateDuplicate.prompt](evaluateDuplicate.prompt)
requests that - compare the result [resultevaluation.txt](resultevaluation.txt).

## How to run it

The script codedupfinder.sh is the entry point, and contains some variables like the source code location if you 
want to try it on other code bases. From that the other scripts are called. This generates various temporary files
in subdirectories of this directory, and the following result files:

- [result.txt](result.txt): the best matches
- [result.json](result.json): the best matches in JSON format, as input for further steps
- [resultevaluation.txt](resultevaluation.txt): LLM rated evaluation.

## Ideas for improvement

The GPT-4o rating is a bit expensive. It might be a good idea to add a step in between where 3.5 is used to extract 
the actual source code of the methods, and then presenting only that to GPT-4o. That would likely be considerably
cheaper and probably faster without losing much quality. OTOH that would prevent GPT-4o to see the context of the
methods when rating them. You might also try a local model for all that - creating an 
[.aigenpipeline file](https://aigenpipeline.stoerr.net/otherModels.html) for that can do this.

## Conclusion

As an example you can find what this outputs in result.txt. Of course, this project is too small to have significant
duplicates. But you can adapt the paths in codedupfinder.sh and try it out on your own project. Have fun trying it out!
