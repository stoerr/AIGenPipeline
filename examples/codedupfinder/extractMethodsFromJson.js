/* AIGenPromptStart(marker)
Print a nodejs script that gets a filename argument with a JSON file like this

```
{
  "hasArgument": "boolean hasArgument(String shortform, String longform) - This method checks ...",
  "AIGenArgumentList": "AIGenArgumentList(String[] args) - Constructor for creating an argument..."
}
```

The script should go through the attributes and write each value to a file that is the name of the JSON file plus
the key plus extension ".descr". That should be in the same subdirectory the JSON file is in. At the
start of the JSON there is one Javascript style multiline comment that should be removed before parsing.
No yabbering!

AIGenCommand(marker)
-m gpt-4o
AIGenPromptEnd(marker) */
/* AIGenVersion(4b3b6959, extractMethodsFromJson.js-5fc084b1) */

const fs = require('fs');
const path = require('path');

if (process.argv.length < 3) {
  console.error('Usage: node script.js <filename>');
  process.exit(1);
}

const filename = process.argv[2];
const filePath = path.resolve(filename);

fs.readFile(filePath, 'utf8', (err, data) => {
  if (err) {
    console.error(`Error reading file: ${err.message}`);
    process.exit(1);
  }

  // Remove the JavaScript style multiline comment
  const jsonString = data.replace(/\/\*[\s\S]*?\*\//, '').trim();

  let jsonData;
  try {
    jsonData = JSON.parse(jsonString);
  } catch (parseErr) {
    console.error(`Error parsing JSON: ${parseErr.message}`);
    process.exit(1);
  }

  const dir = path.dirname(filePath);
  const baseName = path.basename(filename, path.extname(filename));

  for (const [key, value] of Object.entries(jsonData)) {
    const outputFilePath = path.join(dir, `${baseName}${key}.descr`);
    fs.writeFile(outputFilePath, value, 'utf8', (writeErr) => {
      if (writeErr) {
        console.error(`Error writing file: ${writeErr.message}`);
        process.exit(1);
      }
    });
  }
});
