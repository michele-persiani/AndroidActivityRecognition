package umu.software.activityrecognition.speech;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Semaphore;

import umu.software.activityrecognition.common.AndroidUtils;
import umu.software.activityrecognition.common.lifecycles.LifecycleElement;

/**
 * Text-to-Speech singleton for the device. To be used together with LifecycleActivity
 * or LifecycleService
 */
public enum TTS implements LifecycleElement
{
    INSTANCE;


    private TextToSpeech mTextToSpeech;
    Locale mLocale = Locale.getDefault();
    boolean mInitialized = false;
    boolean mInitializing = false;

    Handler mHandler;

    @Override
    public void onCreate(Context context)
    {
        if (mInitialized || mInitializing)
            return;
        mInitializing = true;
        mHandler = AndroidUtils.newHandler();
        mTextToSpeech = new TextToSpeech(context, status -> {
            if(status != TextToSpeech.ERROR)
            {
                mTextToSpeech.setLanguage(mLocale);
                Log.i("TTS", "TTS successfully initialized");
                mInitialized = true;
                mInitializing = false;
            }
            else
                Log.e("TTS", "Error while initializing TTS");
        });


    }

    @Override
    public void onStart(Context context)
    {

    }

    @Override
    public void onStop(Context context)
    {

    }

    @Override
    public void onDestroy(Context context)
    {
        if (!mInitialized)
            return;
        mInitialized = false;
        mTextToSpeech.setOnUtteranceProgressListener(null);
        mTextToSpeech.shutdown();
        mHandler.getLooper().quitSafely();
        mTextToSpeech = null;
        mHandler = null;
    }

    public TTS say(String prompt, UtteranceProgressListener callback)
    {
        mHandler.post(() -> {
            if (!mInitialized)
            {
                callback.onError("null", TextToSpeech.ERROR_NOT_INSTALLED_YET);
                return;
            }
            mTextToSpeech.setOnUtteranceProgressListener(callback);
            mTextToSpeech.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, TTS.class.getName());
        });
        return this;
    }


    public TTS say(String prompt)
    {
        return say(prompt, null);
    }

    public TTS setLocale(Locale locale)
    {
        mHandler.post(() -> {
            if (!mInitialized)
                return;
            mTextToSpeech.setLanguage(locale);
        });
        return this;
    }
}
