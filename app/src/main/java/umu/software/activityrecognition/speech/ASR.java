package umu.software.activityrecognition.speech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import umu.software.activityrecognition.application.ApplicationSingleton;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.permissions.Permissions;


/**
 * Automatic-Speech-Recognition singleton for the device.
 * Requires RECORD_AUDIO permission. See Permissions
 */
public enum ASR
{
    FREE_FORM(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),
    WEB_SEARCH(RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

    private final String mLanguageModel;

    private SpeechRecognizer mSpeechRecognizer;
    private final Handler mHandler = AndroidUtils.newMainLooperHandler();

    private Locale mLocale = Locale.getDefault();
    private Vibrator mVibrator;
    private boolean mListening = false;

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
    }

    public void destroy()
    {
        if (!isInitialized())
            return;
        mSpeechRecognizer.destroy();
        mSpeechRecognizer.setRecognitionListener(null);
        mSpeechRecognizer = null;
        mVibrator = null;
    }

    public boolean isInitialized()
    {
        return mSpeechRecognizer != null;
    }


    public void askPermissions(Activity activity)
    {
        Permissions.RECORD_AUDIO.askPermission(activity);
    }


    public ASR setLanguage(Locale locale)
    {
        mLocale = locale;
        return this;
    }

    public boolean isListening()
    {
        return mListening;
    }

    /***
     * Start listening from the device, eventually translating the recorded speech with the given Translator.
     * For efficiency only results in the call onResults() are translated
     * NB. Only one process at a time can be executing startListening(). When a thread finishes
     * listening it is only by calling stopListening() that the listening semaphore is released
     * @param translator optional translator to use
     * @param listener callback
     */
    public void startListening(Translator translator, RecognitionListener listener)
    {
        if (mSpeechRecognizer == null)
        {
            listener.onError(SpeechRecognizer.ERROR_CLIENT);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, mLanguageModel);
        Locale language = (translator == null)? mLocale : translator.getSourceLanguage();
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, new String[]{});
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);


        mHandler.post(() -> {
            if (mListening)
                return;
            mListening = true;
            RecognitionListener tlistener = getTranslatorListener(listener, translator);
            mSpeechRecognizer.setRecognitionListener(tlistener);
            mSpeechRecognizer.startListening(intent);
            mVibrator.vibrate(
                    VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE)
            );
        });
    }

    /***
     * Start listening from the device.
     * NB. Only one process at a time can be executing startListening(). When a thread finishes
     * listening it is only by calling stopListening() that the listening semaphore is released
     * @param listener callback
     */
    public void startListening(RecognitionListener listener)
    {
       startListening(null, listener);
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
     * Wraps the callback with a translating callback. Translates only results in onResults()
     * @param wrapped the wrapped listener that will be called with the translated recognized speech
     * @param translator optional translator operating the translation. If null no translation will be performed
     * @return the translating listener
     */
    private RecognitionListener getTranslatorListener(RecognitionListener wrapped, @Nullable Translator translator)
    {
        return new RecognitionListener()
        {
            private ArrayList<String> translateResults(Bundle bundle)
            {
                ArrayList<String> results = ASR.getRecognizedSpeech(bundle);
                if(translator != null)
                {
                    String sentence;
                    for (int i = 0; i < results.size(); i++) {
                        sentence = results.get(i);
                        sentence = translator.translate(sentence);
                        results.set(i, sentence);
                    }
                }
                bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, results);
                return results;
            }
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
                translateResults(bundle);
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
