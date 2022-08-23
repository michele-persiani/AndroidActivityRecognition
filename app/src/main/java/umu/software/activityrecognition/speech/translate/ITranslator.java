package umu.software.activityrecognition.speech.translate;

import java.util.Locale;
import java.util.function.Function;


/**
 * Interface for text translators. For example for translating swedish to english
 */
public interface ITranslator extends Function<String, String>
{

    /**
     * Alias for translate()
     * @param text sentence to translate
     * @return translated sentence
     */
    default String apply(String text)
    {
        return translate(text);
    }

    /**
     * Translate a sentence from source to target language
     * @param text sentence to translate
     * @return translated sentence
     */
    String translate(String text);

    /**
     * Returns the source language
     * @return the locale of the source language
     */
    Locale getSourceLanguage();


    /**
     * Returns the target language
     * @return the locale of the target language
     */
    Locale getTargetLanguage();
}
