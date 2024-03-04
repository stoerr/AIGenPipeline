/* AIGenVersion(5ae08828, 3js.prompt-1931e645, README.md-38c61603, dialogelements.txt-64c2ec1a, requests.jsonl-2747fc59) */

// Function to handle the translation request
async function handleTranslationRequest() {
    const apiKey = localStorage.getItem('openai_api_key') || prompt('Please enter your OpenAI API key:');
    
    const request = {
        "model": "gpt-3.5-turbo",
        "messages": [
            {
                "role": "system",
                "content": "You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."
            },
            {
                "role": "user",
                "content": document.getElementById('originalText').value
            },
            {
                "role": "user",
                "content": "Print this complete text translated into ${targetlanguage}. ${addition}"
            }
        ]
    };
    
    try {
        const response = await fetch('https://api.openai.com/v1/chat/completions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + apiKey
            },
            body: JSON.stringify(request)
        });
        
        if (response.status !== 200) {
            const errorResponse = await response.json();
            alert('Error: ' + errorResponse.error.message);
        } else {
            const data = await response.json();
            const translatedText = data.choices[0].message.content;
            document.getElementById('autoTranslatedText').value = translatedText;
        }
    } catch (error) {
        alert('An error occurred while translating the text.');
        console.error(error);
    }
}

// Function to bind the translation request to the Translate Button
document.getElementById('translateButton').addEventListener('click', handleTranslationRequest);
