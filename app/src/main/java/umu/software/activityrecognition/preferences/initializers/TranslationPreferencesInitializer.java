package umu.software.activityrecognition.preferences.initializers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;

import umu.software.activityrecognition.preferences.TranslationPreferences;
import umu.software.activityrecognition.speech.translate.LanguageTranslation;

public class TranslationPreferencesInitializer implements Initializer<TranslationPreferences>
{
    @NonNull
    @Override
    public TranslationPreferences create(@NonNull Context context)
    {
        TranslationPreferences pref = new TranslationPreferences(context);

        for (String source : pref.sourceLanguages().get())
            for (String target : pref.targetLanguages().get())
                downloadModel(source, target);
        return pref;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }


    private void downloadModel(String source, String target)
    {
        LanguageTranslation.INSTANCE.getTranslator(
                Locale.forLanguageTag(source),
                Locale.forLanguageTag(target)
        );
    }
}
