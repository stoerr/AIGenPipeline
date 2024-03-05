/* AIGenVersion(31485649, 3js.prompt-15bb6c39, README.md-0243cfe5, dialogelements.txt-4684af8d, requests.jsonl-10415dab) */

// Function to fetch the API key from localStorage or prompt the user to enter it
function getApiKey() {
    let apiKey = localStorage.getItem('openai_api_key');
    if (!apiKey) {
        apiKey = prompt('Please enter your OpenAI API key:');
        localStorage.setItem('openai_api_key', apiKey);
    }
    return apiKey;
}

// Function to handle the translation request
function handleTranslationRequest() {
    const apiKey = getApiKey();
    const instructions = document.getElementById('instructionsField').value;
    const originalText = document.getElementById('originalTextField').value;

    const requestData = {
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
    };

    fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + apiKey
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (response.status !== 200) {
            alert('Error: Unable to complete translation. Please try again.');
        } else {
            return response.json();
        }
    })
    .then(data => {
        const translatedText = data.choices[0].message.content;
        document.getElementById('autoTranslatedTextField').value = translatedText;
    })
    .catch(error => {
        alert('Error: ' + error.message);
    });
}

// Function to handle the re-translation request
function handleRetranslationRequest() {
    const apiKey = getApiKey();
    const instructions = document.getElementById('instructionsField').value;
    const originalText = document.getElementById('originalTextField').value;
    const correctedText = document.getElementById('correctedTextField').value;
    const revisedText = document.getElementById('changedTextField').value;

    const requestData = {
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
            },
            {
                "role": "assistant",
                "content": correctedText
            },
            {
                "role": "user",
                "content": revisedText
            }
        ]
    };

    fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + apiKey
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        if (response.status !== 200) {
            alert('Error: Unable to complete re-translation. Please try again.');
        } else {
            return response.json();
        }
    })
    .then(data => {
        const retranslatedText = data.choices[0].message.content;
        document.getElementById('retranslatedResultField').value = retranslatedText;
    })
    .catch(error => {
        alert('Error: ' + error.message);
    });
}

// Bind functions to buttons
document.getElementById('translateButton').addEventListener('click', handleTranslationRequest);
document.getElementById('retranslateButton').addEventListener('click', handleRetranslationRequest);
