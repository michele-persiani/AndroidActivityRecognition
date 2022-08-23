package umu.software.activityrecognition.services.chatbot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.startup.AppInitializer;

import java.util.Locale;

import umu.software.activityrecognition.preferences.TranslationPreferences;
import umu.software.activityrecognition.preferences.initializers.TranslationPreferencesInitializer;
import umu.software.activityrecognition.services.LocalBinder;
import umu.software.activityrecognition.services.ServiceConnectionHandler;
import umu.software.activityrecognition.speech.translate.LanguageTranslation;

/**
 * Bound service providing language translation
 */
public class TranslationService extends Service
{
    TranslationPreferences mPreferences;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = AppInitializer.getInstance(this).initializeComponent(
                TranslationPreferencesInitializer.class
        );

        for (String source : mPreferences.sourceLanguages().get())
            for (String target : mPreferences.targetLanguages().get())
                LanguageTranslation.INSTANCE.getTranslator(Locale.forLanguageTag(source), Locale.forLanguageTag(target));
    }

    /**
     * Translates a given sentence from 'source' to 'target' language
     * @param text sentence to translate
     * @param source source language
     * @param target target language
     * @return translated sentence
     */
    public String translate(String text, Locale source, Locale target)
    {
        return LanguageTranslation.INSTANCE.getTranslator(source, target).translate(text);
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        LanguageTranslation.INSTANCE.closeAllCached();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new LocalBinder<>(this);
    }


    /**
     * Helper function to bind this service
     * @param context calling context
     * @return a binding connection handler.
     */
    public static ServiceConnectionHandler<LocalBinder<TranslationService>> bind(Context context)
    {
        return new ServiceConnectionHandler<LocalBinder<TranslationService>>(context).bind(TranslationService.class);
    }
}
