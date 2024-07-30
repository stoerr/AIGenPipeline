#!/usr/bin/env node
/*
This reads the results.json and uses ChatGPT to evaluate on the source whether it is really a duplicate.
Needs three arguments:
- the source directory relative to which we can find the actual files, and
- a directory where we put files with the evaluations
- the path to the results.json
Format of results.json:
[
  {
    "similarity": 0.9127850848115157,
    "id1": "aigenpipeline-framework/src/main/java/net/stoerr/ai/aigenpipeline/framework/chat/OpenAIChatBuilderImpl.java.userMsg.descr",
    "content1": "AIChatBuilder userMsg(String text) is ...",
    "id2": "aigenpipeline-framework/src/main/java/net/stoerr/ai/aigenpipeline/framework/chat/AIChatBuilder.java.userMsg.descr",
    "content2": "AIChatBuilder userMsg(String text) is ..."
  },
  {
    "similarity": 0.9093596199032645,
    "id1": "aigenpipeline-framework/src/main/java/net/stoerr/ai/aigenpipeline/framework/task/AIGenerationTask.java.addInput.descr",
    "content1": "AIGenerationTask addInput(AIInOut input) is ...",
    "id2": "aigenpipeline-framework/src/main/java/net/stoerr/ai/aigenpipeline/framework/task/AIGenerationTask.java.addOptionalInput.descr",
    "content2": "AIGenerationTask addOptionalInput(AIInOut input) is .."
  },
  ...
]
 */
const fs = require('fs');
const path = require('path');
const {spawnSync} = require('child_process');

if (process.argv.length < 5) {
    console.error('Usage: node script.js <sourceDir> <evalDir> <results.json>');
    process.exit(1);
}
const sourceDir = process.argv[2];
const evalDir = process.argv[3];
const resultsJson = process.argv[4];

const results = JSON.parse(fs.readFileSync(resultsJson, 'utf-8'));
const evalFiles = [];

// loop through the results, find the files we we are comparing, and write the evaluation to a file
for (const result of results) {
    // the ids are the filename relative to the source directory, a period and the method name and extension .descr
    // remove these from the id2
    const filename1 = result.id1.split('.').slice(0, -2).join('.');
    const methodname1 = result.id1.split('.').slice(-2, -1).join('.').replaceAll('[^a-zA-Z0-9]+', '_');
    const filename2 = result.id2.split('.').slice(0, -2).join('.');
    const methodname2 = result.id2.split('.').slice(-2, -1).join('.').replaceAll('[^a-zA-Z0-9]+', '_');

    const file1 = path.join(sourceDir, filename1);
    const file2 = path.join(sourceDir, filename2);
    // abort if one of the files doesn't exist - something's wrong when calling this.
    if (!fs.existsSync(file1)) {
        console.error(`File not found: ${file1}`);
        process.exit(1);
    }
    if (!fs.existsSync(file2)) {
        console.error(`File not found: ${file2}`);
        process.exit(1);
    }

    // console.log(`\nComparing ${filename1}#${methodname1} with ${filename2}#${methodname2}`)
    var shortId1 = result.id1.replace(/\//g, '_');
    shortId1 = shortId1.replace(/([^_])[^_]+_/g, '$1_').replaceAll('[^a-zA-Z0-9]+', '_');
    var shortId2 = result.id2.replace(/\//g, '_');
    shortId2 = shortId2.replace(/([^_])[^_]+_/g, '$1_').replaceAll('[^a-zA-Z0-9]+', '_');

    const evalFile = path.join(evalDir, `${shortId1}-${shortId2}.eval`);

    // run aigenpipeline with the two files and write the output to the eval file
    const args = ['-v', '-p', 'evaluateDuplicate.prompt', '-k', `method1=${methodname1}`, '-k', `method2=${methodname2}`, '-k', `file1=${file1}`, '-k', `file2=${file2}`, '-o', evalFile, file1, file2];
    console.log('aigenpipeline', args.join(' '));
    const output = spawnSync('aigenpipeline', args).toString();
    console.log(output);
    evalFiles.push(evalFile);
}

// read in all files matching '*.eval' in the evalDir as JSON and sort by `deduplicated_lines`.
// remove first line from each file since it contains the version comment.
const evals = evalFiles.map(file => JSON.parse(
    fs.readFileSync(file, 'utf-8').split('\n').slice(1).join('\n')
));
evals.sort((a, b) => b.deduplicated_lines - a.deduplicated_lines);
console.log(evals);
