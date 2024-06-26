#!/usr/bin/env node

/* This reads entries from JSONL file db/embeddings.jsonl where each line has the following format (reformatted for visibility):
{
    "id": "1descriptionjson/aigenpipeline-commandline/src/main/java/net/stoerr/ai/aigenpipeline/commandline/AIGenArgumentList.javagetCfgFile.descr",
    "content": "File getCfgFile()\nThis method returns the configuration file from which the arguments were read, if applicable.",
    "embedding": {
      "$base64": true,
      "encoded": "cbOiuzfezjz+uei6QiUMPGADv7y4qdW6pIm0O5h2tzx49Ia8cpnCO7VhWr0Pngy8YOlePCUgCTvU1ag8ohrpPE1tO7xKMjC8wKgXOxoOfjv2kmQ8xYegO3qYhLyPdgM8oGiJPHJxADwYTy68rHuGvEioEj"
    }
  }
  The embedding is a base64 encoded float array. The task is to find the closest embeddings to a given query embedding.
 */

const fs = require('fs');
const path = require('path');
const embeddingsjsonl = path.resolve(__dirname, 'db/embeddings.jsonl');

// read embeddingsjsonl into this map
const embeddings = new Map();

// read embeddingsjsonl into embeddings array
function readEmbeddings() {
    const lines = fs.readFileSync(embeddingsjsonl, 'utf-8').split('\n');
    for (const line of lines) {
        if (line.trim() === '') {
            continue;
        }
        const entry = JSON.parse(line);
        // ignore entries with an entry.id for which the file does not exist (anymore)
        if (!fs.existsSync('descriptions/' + entry.id)) {
            continue;
        }
        // decode entry.embedding.encoded into entry.embedding as a Float32Array
        const buffer = Buffer.from(entry.embedding.encoded, 'base64');
        entry.embedding = new Float32Array(new Uint8Array(buffer).buffer);
        embeddings.set(entry.id, entry);
    }
}
readEmbeddings();

function cosineSimilarity(embedding1, embedding2) {
    let dotProduct = 0;
    let norm1 = 0;
    let norm2 = 0;
    for (let i = 0; i < embedding1.length; i++) {
        dotProduct += embedding1[i] * embedding2[i];
        norm1 += embedding1[i] * embedding1[i];
        norm2 += embedding2[i] * embedding2[i];
    }
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}

// calculate similarities for all pairs of embeddings
/** Objects {id1, id2, similarity} */
const similarities = []

function calculateSimilarities() {
    const keys = Array.from(embeddings.keys());
    for (let i = 0; i < keys.length; i++) {
        const id1 = keys[i];
        let data1 = embeddings.get(id1);
        const embedding1 = data1.embedding;
        for (let j = i + 1; j < keys.length; j++) {
            const id2 = keys[j];
            let data2 = embeddings.get(id2);
            const embedding2 = data2.embedding;
            let similarity = cosineSimilarity(embedding1, embedding2);
            similarities.push({similarity, id1, "content1": data1.content, id2, "content2": data2.content});
        }
    }
}
calculateSimilarities();

// sort similarities by decreasing similarity
similarities.sort((a, b) => b.similarity - a.similarity);

// print the 20 most similar pairs
for (let i = 0; i < 20; i++) {
    const sim = similarities[i];
    console.log(`### Similarity ${sim.similarity}:`);
    console.log(`=== ${sim.id1}:`);
    console.log(`${embeddings.get(sim.id1).content}\n`);
    console.log(`=== ${sim.id2}:`);
    console.log(`${embeddings.get(sim.id2).content}\n\n`);
}

// create a json with an array of the 50 most similar pairs and write it to the file closest.json
const closest = similarities.slice(0, 50);
fs.writeFileSync('results.json', JSON.stringify(closest, null, 2));
