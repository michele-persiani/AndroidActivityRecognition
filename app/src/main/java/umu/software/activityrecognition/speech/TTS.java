package umu.software.activityrecognition.speech;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.api.client.util.Lists;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import umu.software.activityrecognition.shared.util.AndroidUtils;

/**
 * Text-to-Speech singleton for the device.
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


    /**
     * Initialize the text-to-speech. Must be called to use it
     * @param context calling context
     */
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
                mVoice = mTextToSpeech.getDefaultVoice();
                mInitialized = true;
                mInitializing = false;
            }
            else {
                Log.e("TTS", "Error while initializing TTS");
                mInitializing = false;
                mHandler.postDelayed(() -> initialize(context), 2000);
            }
        }, "com.google.android.tts");
    }

    /**
     * Destroys the tts. initialize() must be called again before using it again
     */
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

    /**
     * Returns whether the tts is speaking ie. say() was recently called
     * @return whether the tts is speaking
     */
    public boolean isTalking()
    {
        return mTalking;
    }



    /**
     * Sets the language of the speech. A null value will set the default system language.
     * @param locale the language of the speech
     */
    public void setLanguage(@Nullable Locale locale)
    {
        mLanguage = (locale != null)? locale : Locale.getDefault();
    }

    /**
     * Gets the speech language currently in use
     * @return the speech language currently in use
     */
    public Locale getLanguage()
    {
        return mLanguage;
    }


    /**
     * Utters the given prompt on the device's speakers
     * @param prompt the prompt to say
     * @param callback callback receiving tts events
     */
    public void say(String prompt, UtteranceProgressListener callback)
    {
        mHandler.post(() -> {
            String utteranceId = TTS.class.getName();

            if (!mInitialized)
            {
                callback.onError(utteranceId, TextToSpeech.ERROR);
                return;
            }
            mTextToSpeech.setLanguage(mLanguage);
            if (mVoice != null)
                mTextToSpeech.setVoice(mVoice);
            mTextToSpeech.setOnUtteranceProgressListener(getListener(callback));
            mTextToSpeech.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        });
    }

    /**
     * Returns the default TTS voice
     * @return the default TTS voice
     */
    public Voice getDefaultVoice()
    {
        return mTextToSpeech.getDefaultVoice();
    }

    /**
     * Get the available voices
     * @param filter optional filter to filter returned voices. Can be null
     * @return list of available voices
     */
    public List<Voice> getAvailableVoices(@Nullable Predicate<Voice> filter)
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
        //mLanguage = voice.getLocale();
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

    /**
     * Sets the voice speed
     * @param speechRate a value between 0 and 1 indicating the voice speed
     */
    public void setSpeechRate(float speechRate)
    {
        if (mTextToSpeech != null)
            mTextToSpeech.setSpeechRate(Math.max(0, Math.min(1, speechRate)));
    }

    /**
     * Returns the voice currently in use
     * @return the voice currently in use
     */
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
