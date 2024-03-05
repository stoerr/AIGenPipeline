/* AIGenVersion(195c2445, 4examplejs.prompt-6417c081, README.md-2deb7062, dialogelements.txt-4684af8d) */

// JavaScript code implementing the functionalities related to the drop down list

const examples = {
    "Select an example": {
        instructions: "Translate this text into",
        originalText: "",
        autoTranslatedText: "",
        correctedText: "",
        changedText: ""
    },
    "Example 1": {
        instructions: "Translate this text into German with a formal tone.",
        originalText: "Hello, how are you?",
        autoTranslatedText: "Hallo, wie geht es Ihnen?",
        correctedText: "Guten Tag, wie geht es Ihnen?",
        changedText: "Good morning, how are you?"
    },
    "Example 2": {
        instructions: "Translate this text into English with a casual tone.",
        originalText: "Guten Morgen, wie geht es Ihnen?",
        autoTranslatedText: "Good morning, how are you?",
        correctedText: "Hey, how's it going?",
        changedText: "Hey, how are you doing?"
    },
    "Example 3": {
        instructions: "Translate this text into German with a friendly tone.",
        originalText: "I need help with my homework.",
        autoTranslatedText: "Ich brauche Hilfe bei meinen Hausaufgaben.",
        correctedText: "Kannst du mir bei meinen Hausaufgaben helfen?",
        changedText: "I require assistance with my assignments."
    }
};

const examplesDropdown = document.getElementById("examplesDropdown");
const instructionsField = document.getElementById("instructionsField");
const originalTextField = document.getElementById("originalTextField");
const autoTranslatedTextField = document.getElementById("autoTranslatedTextField");
const correctedTextField = document.getElementById("correctedTextField");
const changedTextField = document.getElementById("changedTextField");

// Populate dropdown with examples
for (const example in examples) {
    const option = document.createElement("option");
    option.text = example;
    examplesDropdown.add(option);
}

// Event listener for dropdown change
examplesDropdown.addEventListener("change", function() {
    const selectedExample = examples[this.value];
    instructionsField.value = selectedExample.instructions;
    originalTextField.value = selectedExample.originalText;
    autoTranslatedTextField.value = selectedExample.autoTranslatedText;
    correctedTextField.value = selectedExample.correctedText;
    changedTextField.value = selectedExample.changedText;
});
