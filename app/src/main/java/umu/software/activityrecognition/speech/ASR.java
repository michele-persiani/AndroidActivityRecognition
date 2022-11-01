package umu.software.activityrecognition.speech;

import android.content.Context;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import umu.software.activityrecognition.shared.util.VibratorManager;

import javax.annotation.Nullable;
import java.util.ArrayList;


/**
 * Automatic-Speech-Recognition singleton for the device.
 * Requires RECORD_AUDIO permission.
 */
public class ASR
{
    private static ASR sInstance;

    private SpeechRecognizer mSpeechRecognizer;
    private Handler mHandler;
    private VibratorManager mVibrator;
    private boolean mListening = false;



    public static ASR getInstance()
    {
        if (sInstance == null)
            sInstance = new ASR();
        return sInstance;
    }


    protected void initialize(Context context)
    {
        if (isInitialized())
            return;
        mHandler = new Handler(Looper.getMainLooper());
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mVibrator = VibratorManager.getInstance(context);
        mListening = false;
    }



    protected void destroy()
    {
        if (!isInitialized())
            return;
        stopListening();
        mSpeechRecognizer.setRecognitionListener(null);
        mSpeechRecognizer.destroy();
        mSpeechRecognizer = null;
        mVibrator = null;
        mListening = false;
    }


    public boolean isInitialized()
    {
        return mSpeechRecognizer != null;
    }



    /**
     * Returns whether the speech recognizer is listening
     * @return whether the speech recognizer is listening
     */
    public boolean isListening()
    {
        return mListening;
    }


    /***
     * Start listening a user utterance from the device's microphone
     * NB. Only one process at a time can be executing listening at any given time. Calling startListening()
     * while the speech recognizer is already running will trigger onError() in the callback
     */
    public void startListening(ListenCommand command)
    {
        mHandler.post(() -> {
            if (mListening || !isInitialized())
            {
                if (command.listener != null)
                    command.listener.onError(SpeechRecognizer.ERROR_CLIENT);
                return;
            }
            mListening = true;
            RecognitionListener translatorListener = wrapListener(command.listener);
            mSpeechRecognizer.setRecognitionListener(translatorListener);
            mSpeechRecognizer.startListening(command.recognizerIntent);
            mVibrator.vibrate(50);

        });
    }


    private void innerListen(ListenCommand command)
    {

    }

    /**
     * Stop listening.
     */
    public void stopListening()
    {
        if (mSpeechRecognizer == null)
            return;
        mHandler.post(() -> {
            if (!isInitialized())
                return;
            mSpeechRecognizer.stopListening();
            mListening = false;
        });
    }


    /**
     * Extract the recognized speech (list of candidates) from the result bundle
     * @param resultBundle result bundle coming from RecognitionListener
     * @return list of candidates recognized speeches
     */
    public static ArrayList<String> getRecognizedSpeech(Bundle resultBundle)
    {
        return resultBundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    }


    /**
     * Extract the confidence scores from the result bundle
     * @param resultBundle result bundle coming from RecognitionListener
     * @return array of confidence scores
     */
    public static float[] getConfidenceScores(Bundle resultBundle)
    {
        return resultBundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
    }

    /**
     * Wraps the callback with a state-tracking logic ie. to provide isListening()
     * @param wrapped the wrapped listener that will be called along the updates to the ASR state
     * @return
     */
    private RecognitionListener wrapListener(@Nullable RecognitionListener wrapped)
    {
        return new RecognitionListener()
        {
            @Override
            public void onReadyForSpeech(Bundle bundle)
            {
                if (wrapped != null) wrapped.onReadyForSpeech(bundle);
            }

            @Override
            public void onBeginningOfSpeech()
            {
                if (wrapped != null) wrapped.onBeginningOfSpeech();
            }

            @Override
            public void onRmsChanged(float v)
            {
                if (wrapped != null) wrapped.onRmsChanged(v);
            }

            @Override
            public void onBufferReceived(byte[] bytes)
            {
                if (wrapped != null) wrapped.onBufferReceived(bytes);
            }

            @Override
            public void onEndOfSpeech()
            {
                if(!mListening)
                    return;
                if (wrapped != null) wrapped.onEndOfSpeech();
            }

            @Override
            public void onError(int i)
            {
                if(!mListening)
                    return;
                mListening = false;
                if (wrapped != null) wrapped.onError(i);
            }

            @Override
            public void onResults(Bundle bundle)
            {
                if(!mListening)
                    return;
                mListening = false;
                if (wrapped != null) wrapped.onResults(bundle);
            }

            @Override
            public void onPartialResults(Bundle bundle)
            {
                if (wrapped != null) wrapped.onPartialResults(bundle);
            }

            @Override
            public void onEvent(int i, Bundle bundle)
            {
                if (wrapped != null) wrapped.onEvent(i, bundle);
            }
        };
    }
}
