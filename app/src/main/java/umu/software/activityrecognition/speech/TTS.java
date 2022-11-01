package umu.software.activityrecognition.speech;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.api.client.util.Lists;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;



/**
 * Text-to-Speech singleton for the device.
 */
public class TTS
{
    private static TTS sInstance;

    private TextToSpeech mTextToSpeech;
    private Handler mHandler;

    private boolean mInitialized = false;
    private boolean mInitializing = false;
    private boolean mTalking = false;
    private CountDownLatch mInitLatch;


    private TTS()
    {
    }


    public static TTS getInstance()
    {
        if (sInstance == null)
            sInstance = new TTS();
        return sInstance;
    }


    /**
     * Initialize the text-to-speech. Must be called to use it
     * @param context calling context
     */
    protected void initialize(Context context)
    {
        if (mInitialized || mInitializing)
            return;
        mInitializing = true;
        HandlerThread handlerThread = new HandlerThread("TTS-Hanlder-Thread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mInitLatch = new CountDownLatch(1);
        mTextToSpeech = new TextToSpeech(context, status -> {
            if(status != TextToSpeech.ERROR)
            {
                Log.i("TTS", "TTS successfully initialized");
                mInitialized = true;
                mInitializing = false;
            }
            else {
                Log.e("TTS", "Error while initializing TTS");
                mInitializing = false;
                mHandler.postDelayed(() -> initialize(context), 2000);
            }
            mInitLatch.countDown();
        }, "com.google.android.tts");
    }



    /**
     * Destroys the tts. initialize() must be called again before using it again
     */
    protected void destroy()
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
     * Utters the given prompt on the device's speakers
     */
    public void say(SpeakCommand command)
    {
        mHandler.post(() -> {
            String utteranceId = TTS.class.getName();
            try {
                mInitLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!mInitialized)
            {
                if (command.listener != null)
                    command.listener.onError(utteranceId, TextToSpeech.ERROR);
                return;
            }
            mTextToSpeech.setLanguage(command.language);
            if (command.voice != null)
                mTextToSpeech.setVoice(command.voice);
            mTextToSpeech.setSpeechRate(command.speechRate);
            mTextToSpeech.setOnUtteranceProgressListener(wrapListener(command.listener));
            mTextToSpeech.speak(command.prompt, command.queueAction, null, utteranceId);
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



    private UtteranceProgressListener wrapListener(@Nullable UtteranceProgressListener wrapped)
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
