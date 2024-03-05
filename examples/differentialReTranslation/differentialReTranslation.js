/* AIGenVersion(28a3069e, 3js.prompt-35043be9, README.md-0243cfe5, dialogelements.txt-4684af8d, requests.jsonl-1d51eb51) */

// Function to handle the translation request
async function handleTranslationRequest(originalText, instructions) {
    const apiKey = localStorage.getItem('openai_api_key') || prompt('Please enter your OpenAI API key:');
    
    const requestOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + apiKey
        },
        body: JSON.stringify({
            "model": "gpt-3.5-turbo",
            "messages": [
                {
                    "role": "system",
                    "content": "You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."
                },
                {
                    "role": "user",
                    "content": originalText
                },
                {
                    "role": "user",
                    "content": instructions
                }
            ]
        })
    };
    
    try {
        const response = await fetch('https://api.openai.com/v1/chat/completions', requestOptions);
        if (response.status !== 200) {
            const errorResponse = await response.json();
            alert('Error: ' + errorResponse.error.message);
        } else {
            const responseData = await response.json();
            const translatedText = responseData.choices[0].message.content;
            // Display the translated text in the appropriate field
            document.getElementById('autoTranslatedTextField').value = translatedText;
        }
    } catch (error) {
        alert('An error occurred while translating the text.');
        console.error(error);
    }
}

// Function to bind the translation request to the Translate Button
document.getElementById('translateButton').addEventListener('click', () => {
    const originalText = document.getElementById('originalTextField').value;
    const instructions = document.getElementById('instructionsField').value;
    handleTranslationRequest(originalText, instructions);
});
