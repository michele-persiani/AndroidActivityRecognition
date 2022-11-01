package umu.software.activityrecognition.speech.translate;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslatorOptions;


import java.io.Closeable;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;





/**
 * Text translator using Google MLKit
 */
public class MLKitTranslator implements Closeable, ITranslator
{
    private final Locale source;
    private final Locale target;
    private final Executor executor;

    private com.google.mlkit.nl.translate.Translator translator;

    private boolean modelDownloaded = false;
    private boolean modelDownloading = false;
    private CountDownLatch downloadedLatch;

    /**
     * Create a translator translating text in the source language into text in the target language
     * @param source source language
     * @param target target language
     */
    MLKitTranslator(Locale source, Locale target)
    {
        this.source = source;
        this.target = target;
        this.executor = Executors.newSingleThreadExecutor();
        this.downloadedLatch = new CountDownLatch(0);
        createTranslator();
    }

    private void createTranslator()
    {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(source.getLanguage())
                .setTargetLanguage(target.getLanguage())
                .setExecutor(executor)
                .build();
        translator = Translation.getClient(options);
    }

    /**
     * Returns whether the translator is downloaded
     * @return whether the translator is downloaded
     */
    public boolean isDownloaded()
    {
        return modelDownloaded;
    }

    /**
     * Returns whether the translator is downloading
     * @return whether the translator is downloading
     */
    public boolean isDownloading()
    {
        return modelDownloading;
    }


    public void download(@Nullable Consumer<DownloadConditions.Builder> downloadConditionBuilder)
    {
        if (modelDownloaded)
            return;
        modelDownloading = true;
        downloadedLatch = new CountDownLatch(1);

        DownloadConditions.Builder conditionsBuilder = new DownloadConditions.Builder();
        if (downloadConditionBuilder != null)
            downloadConditionBuilder.accept(conditionsBuilder);

        translator.downloadModelIfNeeded(conditionsBuilder.build()).addOnSuccessListener(
                executor,
                s -> {
                    Log.i(getClass().getSimpleName(), "Model is downloaded");
                    modelDownloaded = true;
                })
                .addOnFailureListener(
                        executor,
                        exception -> {
                            modelDownloaded = false;
                            Log.w(getClass().getSimpleName(), "DOWNLOAD FAILED");
                            Log.w(getClass().getSimpleName(), exception);
                        })
                .addOnCompleteListener(
                        executor,
                        s -> {
                            modelDownloading = false;
                            downloadedLatch.countDown();
                        }
                );
    }


    public void close()
    {
        translator.close();
        translator = null;
        modelDownloaded = false;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        close();
    }

    /**
     * Translates text from the source language to the target language. Returns null if isInitialized()
     * is false, or if there is an error
     * @param text the text to translate
     * @return the text translated to the target language. Or null if the model is not ready
     */

    public String translate(String text)
    {
        if (translator == null)
            createTranslator();

        try
        {
            downloadedLatch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        CountDownLatch latch = new CountDownLatch(1);

        Task<String> translateTask = translator.translate(text);
        translateTask.addOnCompleteListener(
                executor,
                result -> latch.countDown()
        );

        try
        {
            latch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }


        return translateTask.getResult();
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

    public static MLKitTranslator newInstance(String source, String target)
    {
        return newInstance(Locale.forLanguageTag(source), Locale.forLanguageTag(target));
    }



    /**
     * Create a translator translating text in the source language into text in the target language
     * @param source locale of the source language
     * @param target locale of the target language
     */
    public static MLKitTranslator newInstance(Locale source, Locale target)
    {
        return new MLKitTranslator(source, target);
    }

    /**
     * Creates a new Translator that perform the inverse translation of 'original'
     * @param original the translator to invert
     * @return a new translator performing the inverse translation of 'original'
     */
    public static MLKitTranslator invertSourceDest(MLKitTranslator original)
    {
        return newInstance(original.getTargetLanguage(), original.getSourceLanguage());
    }

}
