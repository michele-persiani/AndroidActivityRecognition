package umu.software.activityrecognition.speech;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.LogHelper;


/**
 * Automatic-Speech-Recognition singleton for the device.
 * Requires RECORD_AUDIO permission.
 */
public enum ASR
{
    FREE_FORM(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),
    WEB_SEARCH(RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);


    class LanguageDetailsChecker extends BroadcastReceiver
    {
        LogHelper mLog = LogHelper.newClassTag("Language Details");
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (getResultCode() != Activity.RESULT_OK)
            {
                mLog.w("Couldn't retrieve language preferences");
                return;
            }
            Bundle results = getResultExtras(true);
            if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
                mLanguagePreference = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                mLog.d("Preferred language (%s)", mLanguagePreference);
            }
            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                mSupportedLanguages = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                mLog.d("Supported languages (%s)", String.join(", ", mSupportedLanguages));
            }
        }
    }

    private final String mLanguageModel;

    private SpeechRecognizer mSpeechRecognizer;
    private final Handler mHandler = AndroidUtils.newMainLooperHandler();

    private Locale mLocale = Locale.getDefault();
    private Vibrator mVibrator;
    private boolean mListening = false;
    private int mMaxResults = 1;

    private String mLanguagePreference = null;
    private List<String> mSupportedLanguages = null;

    ASR(String languageModel)
    {
        mLanguageModel = languageModel;
    }


    public void initialize(Context context)
    {
        if (isInitialized())
            return;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mVibrator = AndroidUtils.getVibrator(context);
        mListening = false;
        getSupportedLanguages(context);
    }


    public void getSupportedLanguages(Context context)
    {
        Intent detailsIntent =  new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        context.sendOrderedBroadcast(
                detailsIntent,
                null,
                new LanguageDetailsChecker(),
                null,
                Activity.RESULT_OK,
                null,
                null
        );
    }


    public void destroy()
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
     * Sets the speech language. Input speech is expected in this language
     * @param locale the language to use
     */
    public void setLanguage(Locale locale)
    {
        mLocale = locale;
    }


    /**
     * Gets the currently recognized language. Can be changed through setLanguage()
     * @return the currently recognized language
     */
    public Locale getLanguage()
    {
        return mLocale;
    }


    /**
     * Sets the maximum number of results for a speech recognition
     * @param maxResults maximum number of results
     */
    public void setMaxRecognitionResults(int maxResults)
    {
        mMaxResults = Math.max(1, Math.min(maxResults, 10));
    }


    /**
     * Returns the maximum number of results for a speech recognition
     * @return the maximum number of results for a speech recognition
     */
    public int getMaxRecognitionResults()
    {
        return mMaxResults;
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
     * @param listener callback
     */
    public void startListening(RecognitionListener listener)
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, mLanguageModel);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLocale.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, new String[]{});
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, mMaxResults);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);

        mHandler.post(() -> {
            if (mListening || ! isInitialized())
            {
                listener.onError(SpeechRecognizer.ERROR_CLIENT);
                return;
            }
            mListening = true;
            RecognitionListener translatorListener = wrapListener(listener);
            mSpeechRecognizer.setRecognitionListener(translatorListener);
            mSpeechRecognizer.startListening(intent);
            mVibrator.vibrate(
                    VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE)
            );
        });
    }


    /**
     * Stop listening.
     */
    public void stopListening()
    {
        if (mSpeechRecognizer == null)
            return;
        mHandler.post(() -> {
            mSpeechRecognizer.stopListening();
            mListening = false;
            mVibrator.vibrate(
                    VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE)
            );
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
     * @return the translating listener
     */
    private RecognitionListener wrapListener(RecognitionListener wrapped)
    {
        return new RecognitionListener()
        {
            @Override
            public void onReadyForSpeech(Bundle bundle)
            {
                wrapped.onReadyForSpeech(bundle);
            }

            @Override
            public void onBeginningOfSpeech()
            {
                wrapped.onBeginningOfSpeech();
            }

            @Override
            public void onRmsChanged(float v)
            {
                wrapped.onRmsChanged(v);
            }

            @Override
            public void onBufferReceived(byte[] bytes)
            {
                wrapped.onBufferReceived(bytes);
            }

            @Override
            public void onEndOfSpeech()
            {
                if(!mListening)
                    return;
                wrapped.onEndOfSpeech();
            }

            @Override
            public void onError(int i)
            {
                if(!mListening)
                    return;
                mListening = false;
                wrapped.onError(i);
            }

            @Override
            public void onResults(Bundle bundle)
            {
                if(!mListening)
                    return;
                mListening = false;
                wrapped.onResults(bundle);
            }

            @Override
            public void onPartialResults(Bundle bundle)
            {
                wrapped.onPartialResults(bundle);
            }

            @Override
            public void onEvent(int i, Bundle bundle)
            {
                wrapped.onEvent(i, bundle);
            }
        };
    }
}
