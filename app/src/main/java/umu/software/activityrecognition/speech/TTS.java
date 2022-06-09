package umu.software.activityrecognition.speech;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.google.api.client.util.Lists;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import umu.software.activityrecognition.shared.AndroidUtils;

/**
 * Text-to-Speech singleton for the device.
 *
 */
public enum TTS
{
    INSTANCE;

    private TextToSpeech mTextToSpeech;
    private boolean mInitialized = false;
    private boolean mInitializing = false;

    private Handler mHandler;

    private Voice mVoice;
    private Locale mLanguage = Locale.getDefault();
    private boolean mTalking = false;



    public void initialize(Context context)
    {
        if (mInitialized || mInitializing)
            return;
        mInitializing = true;
        mHandler = AndroidUtils.newHandler();
        mTextToSpeech = new TextToSpeech(context, status -> {
            if(status != TextToSpeech.ERROR)
            {
                mTextToSpeech.setLanguage(mLanguage);
                Log.i("TTS", "TTS successfully initialized");
                mInitialized = true;
                mInitializing = false;
                if(mVoice == null)
                    mVoice = mTextToSpeech.getDefaultVoice();
                setVoice(mVoice);
            }
            else
                Log.e("TTS", "Error while initializing TTS");
        }, "com.google.android.tts");
    }

    public void destroy()
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



    public boolean isTalking()
    {
        return mTalking;
    }


    public void say(String prompt, Translator translator, UtteranceProgressListener callback)
    {
        mHandler.post(() -> {
            String utternaceId = TTS.class.getName();
            String translatedPrompt = prompt;
            Locale currentLanguage = getLanguage();

            if (!mInitialized || (translator != null && !translator.isInitialized()))
            {
                callback.onError(utternaceId, TextToSpeech.ERROR);
                return;
            }
            setVoice(mVoice);
            if (translator != null)
            {
                translatedPrompt = translator.translate(prompt);
                setLanguage(translator.getTargetLanguage());
            }
            mTextToSpeech.setOnUtteranceProgressListener(getListener(callback));
            mTextToSpeech.speak(translatedPrompt, TextToSpeech.QUEUE_FLUSH, null, utternaceId);
            setLanguage(currentLanguage);
        });
    }


    public void say(String prompt, UtteranceProgressListener callback)
    {
        say(prompt, null, callback);
    }


    /**
     * Sets the language of the speech. A null value will set the default language.
     * Note that language and voice can be set independently.
     * @param locale the language of the speech
     */
    public void setLanguage(Locale locale)
    {
        mLanguage = (locale != null)? locale : Locale.getDefault();
        if (mInitialized)
            mTextToSpeech.setLanguage(locale);
    }

    /**
     * Gets the language currently in use
     * @return the language currently in use
     */
    public Locale getLanguage()
    {
        return mLanguage;
    }


    /**
     * Get the available voices
     * @param filter optional filter to filter returned voices. Can be null
     * @return list of available voices
     */
    public List<Voice> getAvailableVoices(Predicate<Voice> filter)
    {
        if (mTextToSpeech == null)
            return Lists.newArrayList();
        if (filter == null)
            filter = (v) -> true;
        Set<Voice> voices = mTextToSpeech.getVoices();
        if (voices == null)
            return Lists.newArrayList();
        return Lists.newArrayList(voices.stream().filter(filter).iterator());
    }

    /**
     * Sets the voice and the language using the voice's language
     * @param voice the voice (and language) to use
     */
    public void setVoice(Voice voice)
    {
        mVoice = voice;
        mLanguage = voice.getLocale();
        if (mTextToSpeech != null)
            mTextToSpeech.setVoice(voice);
    }

    /**
     * Sets the voice to use by its name. The name will be matched with names from the voices returned by getAvailableVoices(null)
     * @param voiceName the name of the voice
     */
    public void setVoice(String voiceName)
    {
        List<Voice> voices = getAvailableVoices(null);
        for (Voice v : voices)
            if(v.getName().equals(voiceName))
                setVoice(v);
    }


    public void setSpeechRate(float speechRate)
    {
        if (mTextToSpeech != null)
            mTextToSpeech.setSpeechRate(Math.max(0, speechRate));
    }


    public Voice getCurrentVoice()
    {
        return mVoice;
    }


    private UtteranceProgressListener getListener(UtteranceProgressListener wrapped)
    {
        return new UtteranceProgressListener()
        {
            @Override
            public void onStart(String s)
            {
                mTalking = true;
                if (wrapped != null) wrapped.onStart(s);
            }

            @Override
            public void onDone(String s)
            {
                mTalking = false;
                if (wrapped != null) wrapped.onDone(s);
            }

            @Override
            public void onError(String s)
            {
                mTalking = false;
                if (wrapped != null) wrapped.onError(s);
            }
        };
    }
}
