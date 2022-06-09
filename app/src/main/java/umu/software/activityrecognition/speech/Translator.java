package umu.software.activityrecognition.speech;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.Closeable;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import umu.software.activityrecognition.shared.Exceptions;

/**
 * Translator for text.
 */
public class Translator implements Closeable
{
    private final Locale source;
    private final Locale target;

    private com.google.mlkit.nl.translate.Translator translator;

    private boolean modelDownloaded = false;

    /**
     * Create a translator translating text in the source language into text in the target language
     * @param source source language
     * @param target target language
     */
    Translator(Locale source, Locale target)
    {
        this.source = source;
        this.target = target;
        initialize();
    }


    /**
     * Returns whether the translator is ready to be used
     * @return whether the translator is ready to be used
     */
    public boolean isInitialized()
    {
        return translator != null && modelDownloaded;
    }


    private void initialize()
    {
        if (isInitialized())
            return;
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(source.getLanguage())
                .setTargetLanguage(target.getLanguage())
                .build();
        translator = Translation.getClient(options);
        translator.downloadModelIfNeeded().addOnSuccessListener((s) -> {
            Log.i(getClass().getSimpleName(), "Download complete");
            modelDownloaded = true;
        });
    }


    public void close()
    {
        if (translator != null)
        {
            translator.close();
            translator = null;
        }
    }

    /**
     * Translates text from the source language to the target language. Returns null if isInitialized()
     * returns false
     * @param text the text to translate
     * @return the text translated to the target language. Or null if the model is not ready
     */
    @Nullable
    public String translate(String text)
    {
        if (getSourceLanguage().equals(getTargetLanguage()))
            return text;
        if (!isInitialized())
            return null;

        Task<String> s = translator.translate(text);

        while (!s.isComplete())
            Exceptions.runCatch(() -> Thread.sleep(10));

        return s.getResult();
    }


    /**
     * Returns the source language
     * @return the locale of the source language
     */
    public Locale getSourceLanguage()
    {
        return source;
    }


    /**
     * Returns the target language
     * @return the locale of the target language
     */
    public Locale getTargetLanguage()
    {
        return target;
    }



    /* --- Factory methods --- */

    public static Translator newInstance(String source, String target)
    {
        return newInstance(Locale.forLanguageTag(source), Locale.forLanguageTag(target));
    }



    /**
     * Create a translator translating text in the source language into text in the target language
     * @param source locale of the source language
     * @param target locale of the target language
     */
    public static Translator newInstance(Locale source, Locale target)
    {
        return new Translator(source, target);
    }

    /**
     * Creates a translator that perform the inverse translation
     * @param original the translator to invert
     * @return a new translator performing the inverse translation of 'original'
     */
    public static Translator invertSourceDest(Translator original)
    {
        return newInstance(original.getTargetLanguage(), original.getSourceLanguage());
    }
}
