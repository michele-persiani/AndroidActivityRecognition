package umu.software.activityrecognition.speech;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.UtteranceProgressListener;

import java.util.List;


/**
 * Singletons asking a question to the user and recording the answer.
 * !! Uses TTS.INSTANCE and ASR.FREE_FORM, so make sure they're started before using this class !!
 */
public enum UserPrompt
{
    INSTANCE;

    public interface Callback
    {
        void onStartSpeaking();
        void onSpeakingDone();
        void onStartListening();
        void onListeningDone();
        void onResult(List<String> answerCandidates);
        void onError(int error);
    }


    private boolean mListening = false;
    private boolean mSpeaking = false;


    public boolean isSpeaking()
    {
        return mSpeaking;
    }


    public boolean isListening()
    {
        return mListening;
    }

    /**
     * Prompt the user and listen to his/her answer. The answer can be later accessed through getLastAnswer()
     * @param prompt The prompt to ask the user
     * @param callback callback
     */
    public void prompt(String prompt, UserPrompt.Callback callback)
    {
        if (mSpeaking || mListening)
        {
            return;
        }
        callback.onStartSpeaking();
        mSpeaking = true;
        TTS.INSTANCE.say(prompt, new UtteranceProgressListener()
        {
            @Override
            public void onStart(String s)
            {

            }

            @Override
            public void onDone(String s)
            {
                callback.onSpeakingDone();
                mSpeaking = false;
                startListening(callback);
            }

            @Override
            public void onError(String s)
            {
                mSpeaking = false;
                callback.onError(SpeechRecognizer.ERROR_CLIENT);
            }
        });
    }

    /**
     * Listen for the user's answer. Called by prompt()
     */
    private void startListening(UserPrompt.Callback callback)
    {
        mListening = true;
        ASR.FREE_FORM.startListening(new RecognitionListener()
        {

            @Override
            public void onReadyForSpeech(Bundle bundle)
            {
                callback.onStartListening();
            }

            @Override
            public void onBeginningOfSpeech()
            {

            }

            @Override
            public void onRmsChanged(float v)
            {

            }

            @Override
            public void onBufferReceived(byte[] bytes)
            {

            }

            @Override
            public void onEndOfSpeech()
            {
                mListening = false;
                callback.onListeningDone();
            }

            @Override
            public void onError(int i)
            {
                mListening = false;
                callback.onError(i);
            }

            @Override
            public void onResults(Bundle bundle)
            {
                mListening = false;
                List<String> answer = ASR.getRecognizedSpeech(bundle);
                callback.onResult(answer);
            }

            @Override
            public void onPartialResults(Bundle bundle)
            {

            }

            @Override
            public void onEvent(int i, Bundle bundle)
            {

            }
        });
    }


}
