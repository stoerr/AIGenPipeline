# Differential re-translation demonstration

## Basic idea

This application is a demonstration of an interesting idea how to support automatic translation with LLM that uses
the advanced reasoning capabilities of the AI to support the translation process of a site. It allows you to explore 
what texts an AI would generate, given a full implementation of this concept in an application.

First, LLM can be given additional instructions for translation that can go far beyond "translate this text into that
language": it can be given instructions to influence the writing style, finetuning the translation to the desired
audience - as e.g. different levels of formality in addressing the reader like "Du" vs. "Sie" in German, or even to give
background information about the site and the translation, if needed.

Second, when the original text is changed, automatically translated, and then manually corrected, the manual corrections
would be lost when the original text is simply retranslated. However, if you give the AI data about the manual
correction, it can try to replicate that correction for the translation of the changed original. The AI would be
given the following data:

- general instructions for the translation including the discussed additional instructions
- the original text
- the automatically translated original text
- the manually corrected translation
- the changed original text

The AI would then be asked to translate the changed original text, observing the general instructions and also trying to
replicate the manual correction. This application can be used to explore this concept: it can be used to create the
translation of an original text, and, after adding some manual corrections, to apply such a differential re-translation.

In addition to the mentioned text fields the application has a button "Translate" after the original text that reads
the general instructions and the original text and writes the translation into the "automatically translated original
text", a "Differential re-translation" button that performs this action, and an "Examples" at the top that drop down list that fills 
the fields with one of several predefined examples.

## List of dialog elements

In this dialog appear the following elements, in the order from top to bottom:

1. **Examples Dropdown**: Allows users to select from predefined examples to fill in the translation fields automatically. This feature is designed to demonstrate the capabilities of the application without requiring the user to input original texts manually.

2. **General Instructions Field**: A text input area where users can specify general instructions for the translation. These instructions can include the desired writing style, target audience considerations, and any additional context or background information relevant to the translation task.

3. **Original Text Field**: A text input area for the user to enter the text that needs to be translated. This field represents the source material in its original language.

4. **Translate Button**: Positioned after the Original Text Field, this button initiates the automatic translation of the text entered in the Original Text Field based on the General Instructions provided. 

5. **Automatically Translated Original Text Field**: Displays the result of the automatic translation of the original text. This field is automatically populated after the user clicks the Translate button.

6. **Manually Corrected Translation Field**: A text input area where users can make manual corrections to the automatically translated text. This allows for human intervention to improve accuracy, fluency, or to better align the translation with the General Instructions.

7. **Changed Original Text Field**: A text input area where users can enter a revised version of the Original Text. This field is used to demonstrate how the application can adapt translations based on changes to the source material.

8. **Differential Re-translation Button**: When clicked, this button initiates the process of translating the Changed Original Text, taking into account the General Instructions, the original Automatic Translation, and any Manual Corrections made. The goal is to produce a new translation that incorporates the essence of the manual corrections while adapting to the changes in the original text.

9. **Result of re-translation**: Displays the result of the differential re-translation. This field is automatically 
   populated after the user clicks the Differential Re-translation button.

## Dialog layout

The full dialog can look like this. A headline and introductory text is at the top, then the dialog fields appear. 
Text fields are given a label and a one sentence description.

### Dialog Layout

**Headline:** Advanced Translation Assistance Tool

**Introductory Text:** Explore the future of language translation with our cutting-edge tool. By combining automatic translation with human insight, this application enhances the accuracy and relevance of translations across various languages and contexts. Perfect for professionals and language enthusiasts alike.

1. **Examples Dropdown**  
   *Label: Choose an Example*  
   *Description: Select from predefined scenarios to automatically populate the translation fields and explore different translation challenges and solutions.*

2. **General Instructions Field**  
   *Label: General Instructions for Translation*  
   *Description: Enter any specific requirements for the translation, such as tone, formality level, or additional contextual information.*

3. **Original Text Field**  
   *Label: Original Text*  
   *Description: Input the text you wish to translate. This will serve as the base for the initial automatic translation.*

4. **Translate Button**  
   *Description: Click to translate the original text according to the provided general instructions.*

5. **Automatically Translated Original Text Field**  
   *Label: Automatically Translated Text*  
   *Description: View the initial translation of your original text. This text is generated based on the general instructions.*

6. **Manually Corrected Translation Field**  
   *Label: Manually Corrected Translation*  
   *Description: Make adjustments to the automatic translation to better meet your requirements or correct inaccuracies.*

7. **Changed Original Text Field**  
   *Label: Revised Original Text*  
   *Description: Enter a modified version of the original text to see how the application adapts the translation to reflect these changes.*

8. **Differential Re-translation Button**  
   *Description: Generate a new translation of the revised text, incorporating your manual corrections and adhering to the initial instructions.*

9. **Result of re-translation**
   *Label: Result of differential re-translation**
   *Description: View the result of the translation of the changed original text while trying to maintain the manual 
   corrections.
