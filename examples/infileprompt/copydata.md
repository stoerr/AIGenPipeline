<!-- AIGenPromptStart(copydata)
This uses the copy fake model to just copy the data.txt file. 
AIGenCommand(copydata)
-m copy -wo -f data.txt
AIGenPromptEnd(copydata) -->
Name: Franz
Profession: Frontend developer

Name: Karl
Profession: Backend developer

Name: Maria
Profession: Designer
<!-- AIGenEnd(copydata) -->

<!-- AIGenPromptStart(openaijson)
This uses the openaijson fake model to just insert the JSON that would have been sent to OpenAI into the file. 
AIGenCommand(openaijson)
-m openaijson -wo -f data.txt
AIGenPromptEnd(openaijson) -->
{
  "model": "openaijson",
  "messages": [
    {
      "role": "system",
      "content": "You are an expert programming assistant who follows the users instructions exactly and to the letter.\nYou observe the rules of clean code, KISS, YAGNI, and DRY and love good documentation and well documented code.\nDo never ever give any introductory or concluding text, just the requested output, except if explicitly asked for.\n\nIMPORTANT: If something in the instructions is unclear, there is information missing, you have any questions\nor after printing the response you notice that something was wrong, then append to the file a paragraph starting with\n`FIXME(GenAIPipeline) ` and a description of that problem.\n"
    },
    {
      "role": "user",
      "content": "Retrieve the content of the input file 'data.txt'"
    },
    {
      "role": "assistant",
      "content": "Name: Franz\nProfession: Frontend developer\n\nName: Karl\nProfession: Backend developer\n\nName: Maria\nProfession: Designer\n"
    },
    {
      "role": "user",
      "content": "This uses the copy fake model to just copy the data.txt file. \n"
    }
  ],
  "temperature": 0.0,
  "max_tokens": 2048
}
