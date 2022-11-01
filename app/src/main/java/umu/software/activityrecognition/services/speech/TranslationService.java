package umu.software.activityrecognition.services.speech;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.google.common.collect.Sets;
import umu.software.activityrecognition.R;
import umu.software.activityrecognition.preferences.TranslationPreferences;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.util.FunctionLock;
import umu.software.activityrecognition.speech.translate.MLKitTranslation;

import java.util.Locale;



/**
 * Bound service providing language translation
 */
public class TranslationService extends LifecycleService
{
    TranslationPreferences mPreferences;

    private final FunctionLock mLock = FunctionLock.newInstance();




    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = new TranslationPreferences(this);

        mPreferences.sourceLanguages().init(
                Sets.newHashSet(getResources().getStringArray(R.array.translator_default_source_languages))
        );
        mPreferences.targetLanguages().init(
                Sets.newHashSet(getResources().getStringArray(R.array.translator_default_target_languages))
        );


       runAsync(() -> {
            mLock.lock();
            for (String source : mPreferences.sourceLanguages().get())
                for (String target : mPreferences.targetLanguages().get())
                    MLKitTranslation.getInstance().getTranslator(
                                    Locale.forLanguageTag(source),
                                    Locale.forLanguageTag(target))
                            .translate("."
                            );
            mLock.unlock();
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mPreferences.clearListeners();
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
        return mLock.withLock(() -> MLKitTranslation.getInstance().getTranslator(source, target).translate(text));
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mLock.lock();
        MLKitTranslation.getInstance().closeAllCached();
        mLock.unlock();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new ServiceBinder<>(this);
    }


    /**
     * Helper function to bind this service
     * @param context calling context
     * @return a binding connection handler.
     */
    public static ServiceConnectionHandler<ServiceBinder<TranslationService>> bind(Context context)
    {
        return new ServiceConnectionHandler<ServiceBinder<TranslationService>>(context).bind(TranslationService.class);
    }
}
