package umu.software.activityrecognition.speech.translate;

import androidx.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.mlkit.common.model.DownloadConditions;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Singleton to retrieve ITranslator objects.
 */
public enum LanguageTranslation
{
    INSTANCE;

    Map<String, MLKitTranslator> translators = Maps.newHashMap();

    /**
     * Gets a translator from 'source' to 'target' language.
     * @param source source language
     * @param target target language
     * @return a new or cached version of the translator
     */
    public ITranslator getTranslator(Locale source, Locale target)
    {
        if (source.getLanguage().equals(target.getLanguage()))
            return new SameLanguageTranslator(source);


        String key = getTranslatorKey(source, target);
        if (translators.containsKey(key))
            return translators.get(key);

        MLKitTranslator translator = MLKitTranslator.newInstance(source, target);
        translator.download(null);
        translators.put(key, translator);
        return translator;
    }

    /**
     * Retrieve a new or cached translator having source and target language inverted from those provided
     * @param inverted the translator of which source and target language should be inverted
     * @return a new or cached translator
     */
    public ITranslator invertSourceWithTarget(ITranslator inverted)
    {
        return getTranslator(inverted.getTargetLanguage(), inverted.getTargetLanguage());
    }

    /**
     * Download all cached translators if they require
     * @param conditionsBuilder optional builder to specify the download conditions
     */
    public void downloadAllCached(@Nullable Consumer<DownloadConditions.Builder> conditionsBuilder)
    {
        for (MLKitTranslator tr : translators.values())
            tr.download(conditionsBuilder);
    }


    /**
     * Close all cached translators releasing the associated resources
     */
    public void closeAllCached()
    {
        for (MLKitTranslator tr : translators.values())
            tr.close();
    }


    private String getTranslatorKey(Locale source, Locale target)
    {
        return String.format("%s_%s", source.toLanguageTag(), target.toLanguageTag());
    }
}
