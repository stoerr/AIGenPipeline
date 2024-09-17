#!/usr/bin/env ../../bin/aigenpipeline
-m gpt-4o-mini -p 0dialogelements.prompt README.md -o dialogelements.txt

-m gpt-4o-mini -p 1html.prompt README.md dialogelements.txt -o differentialReTranslation.html

-m gpt-4o-mini -p 2css.prompt README.md differentialReTranslation.html -o differentialReTranslation.css

-m gpt-4o -p 3js.prompt README.md dialogelements.txt requests.jsonl -o differentialReTranslation.js

-m gpt-4o-mini -p 4examplejs.prompt README.md dialogelements.txt examples.txt -o differentialReTranslationExamples.js
