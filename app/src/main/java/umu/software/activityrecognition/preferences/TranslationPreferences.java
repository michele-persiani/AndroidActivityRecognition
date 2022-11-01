package umu.software.activityrecognition.preferences;

import android.content.Context;

import com.google.common.collect.Sets;

import java.util.Locale;
import java.util.Set;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesInitializer;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;
import umu.software.activityrecognition.speech.translate.MLKitTranslation;

public class TranslationPreferences extends PreferencesModule
{
    static {
        PreferencesInitializer.addInitialization(TranslationPreferences.class);
    }


    public TranslationPreferences(Context context)
    {
        super(context);
    }

    @Override
    protected void initialize()
    {
        sourceLanguages().init(
                Sets.newHashSet(getResources().getStringArray(R.array.translator_default_source_languages))
        );
        targetLanguages().init(
                Sets.newHashSet(getResources().getStringArray(R.array.translator_default_target_languages))
        );
        for (String source : sourceLanguages().get())
            for (String target : targetLanguages().get())
                downloadModel(source, target);
    }


    public Preference<Set<String>> sourceLanguages()
    {
        return getStringSet(R.string.translator_source_languages);
    }


    public Preference<Set<String>> targetLanguages()
    {
        return getStringSet(R.string.translator_target_languages);
    }


    private void downloadModel(String source, String target)
    {
        MLKitTranslation.getInstance().getTranslator(
                Locale.forLanguageTag(source),
                Locale.forLanguageTag(target)
        );
    }
}
