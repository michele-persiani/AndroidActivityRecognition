package umu.software.activityrecognition.speech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.List;
import java.util.Locale;

import umu.software.activityrecognition.common.lifecycles.LifecycleElement;
import umu.software.activityrecognition.common.permissions.Permissions;


/**
 * Automatic-Speech-Recognition singleton for the device. To be used together with LifecycleActivity
 * or LifecycleService
 * Requires RECORD_AUDIO permission. See Permissions
 */
public enum ASR implements LifecycleElement
{
    FREE_FORM(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),
    WEB_SEARCH(RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

    private final String mLanguageModel;

    private SpeechRecognizer mSpeechRecognizer;

    ASR(String languageModel)
    {
        mLanguageModel = languageModel;
    }

    @Override
    public void onCreate(Context context)
    {
        if (mSpeechRecognizer != null)
            return;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
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
        if (mSpeechRecognizer == null)
            return;
        mSpeechRecognizer.destroy();
        mSpeechRecognizer.setRecognitionListener(null);
        mSpeechRecognizer = null;
    }

    public void askPermissions(Activity activity)
    {
        Permissions.RECORD_AUDIO.askPermission(activity);
    }

    /***
     * Start listening from the device.
     * NB. Only one process at a time can be executing startListening(). When a thread finishes
     * listening it is only by calling stopListening() that the listening semaphore is released
     * @param listener callback
     */
    public void startListening(RecognitionListener listener)
    {
        if (mSpeechRecognizer == null)
        {
            listener.onError(SpeechRecognizer.ERROR_CLIENT);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, mLanguageModel);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        mSpeechRecognizer.setRecognitionListener(listener);
        mSpeechRecognizer.startListening(intent);
    }


    /**
     * Stop listening.
     */
    public void stopListening()
    {
        if (mSpeechRecognizer == null)
            return;
        mSpeechRecognizer.stopListening();
    }


    /**
     * Extract the recognized speech (list of candidates) from the result bundle
     * @param resultBundle result bundle coming from RecognitionListener
     * @return list of candidates recognized speeches
     */
    public List<String> getRecognizedSpeech(Bundle resultBundle)
    {
        return resultBundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    }


    /**
     * Extract the confidence scores from the result bundle
     * @param resultBundle result bundle coming from RecognitionListener
     * @return array of confidence scores
     */
    public float[] getConfidenceScores(Bundle resultBundle)
    {
        return resultBundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
    }
}
