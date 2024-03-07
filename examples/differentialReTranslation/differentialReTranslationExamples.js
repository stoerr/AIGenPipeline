/* AIGenVersion(0d13d858, 4examplejs.prompt-57d98546, README.md-1.0, dialogelements.txt-4684af8d, examples.txt-7cca954e) */

// Define the examples data
const examples = [
    {
        name: "Our Contributors",
        originalText: "Our Contributors",
        instructions: "Translate this text into German",
        autoTranslatedText: "Unsere Mitwirkenden",
        manuallyCorrectedText: "Unsere Autoren",
        changedText: "Meet our contributors",
        retranslatedResult: "Treffe unsere Autoren"
    },
    {
        name: "Travel Guides",
        originalText: "Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book.",
        instructions: "Übersetze den Text auf Deutsch, addressiere mit Du.",
        autoTranslatedText: "Lerne unsere außergewöhnlichen Reiseführer kennen. Wenn du mit einem zertifizierten WKND-Führer reist, erhältst du Zugang zu Sehenswürdigkeiten und Perspektiven, die nicht in Reiseführern zu finden sind.",
        manuallyCorrectedText: "Lerne unsere außergewöhnlichen Reiseleiter kennen. Wenn du mit einem zertifizierten WKND-Reiseleiter reist, erhältst du Zugang zu Sehenswürdigkeiten und Perspektiven, die nicht in Reiseführern zu finden sind.",
        changedText: "Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book.",
        retranslatedResult: "Lerne unsere außergewöhnlichen Reiseleiter kennen. Wenn du mit einem zertifizierten WKND-Reiseleiter reist, erhältst du Zugang zu Sehenswürdigkeiten und Perspektiven, die nicht in Reiseführern zu finden sind."
    },
    {
        name: "Sustainable living",
        originalText: "Discover the latest trends in sustainable living.",
        instructions: "Translate to German, aiming for a young, eco-conscious audience. Use informal 'du' and include relevant cultural references to Germany.",
        autoTranslatedText: "Entdecke die neuesten Trends im nachhaltigen Leben.",
        manuallyCorrectedText: "Entdecke die neuesten Trends für ein umweltbewusstes Leben.",
        changedText: "Explore the latest trends in sustainable living in urban environments.",
        retranslatedResult: "Entdecke die neuesten Trends für ein umweltbewusstes Leben in städtischen Umgebungen."
    }
];

// Populate the dropdown list with examples
const dropdown = document.getElementById('examplesDropdown');

examples.forEach((example, index) => {
    const option = document.createElement('option');
    option.text = example.name;
    option.value = index;
    dropdown.add(option);
});

// Event listener for dropdown selection
dropdown.addEventListener('change', function() {
    const selectedExample = examples[this.value];
    
    // Fill the form fields with the selected example data
    document.getElementById('originalTextField').value = selectedExample.originalText;
    document.getElementById('instructionsField').value = selectedExample.instructions;
    document.getElementById('autoTranslatedTextField').value = selectedExample.autoTranslatedText;
    document.getElementById('correctedTextField').value = selectedExample.manuallyCorrectedText;
    document.getElementById('changedTextField').value = selectedExample.changedText;
    document.getElementById('retranslatedResultField').value = selectedExample.retranslatedResult;
});
