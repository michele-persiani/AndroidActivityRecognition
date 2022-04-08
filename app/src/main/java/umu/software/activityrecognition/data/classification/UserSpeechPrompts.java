package umu.software.activityrecognition.data.classification;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.common.lifecycles.LifecycleElement;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.TTS;

public enum UserSpeechPrompts implements LifecycleElement
{
    USER_ACTIVITY_CLASSIFICATION(R.string.request_user_classification);

    private final int mRequestStringId;
    private String mRequestString = null;


    List<String> mAnswerCandidates = new ArrayList<>();
    private boolean mListening = false;
    private boolean mTalking = false;


    UserSpeechPrompts(int requestStringId)
    {
        mRequestStringId = requestStringId;
    }


    public boolean initialized()
    {
        return mRequestString != null;
    }

    @Override
    public void onCreate(Context context)
    {
        if (initialized())
            return;
        ASR.FREE_FORM.onCreate(context);
        TTS.INSTANCE.onCreate(context);
        mRequestString = context.getString(mRequestStringId);
        mTalking = false;
        mListening = false;
    }

    @Override
    public void onStart(Context context)
    {
        if (!initialized())
            return;
        ASR.FREE_FORM.onStart(context);
        TTS.INSTANCE.onStart(context);
    }

    @Override
    public void onStop(Context context)
    {
        if (!initialized())
            return;
        ASR.FREE_FORM.onStop(context);
        TTS.INSTANCE.onStop(context);
    }

    @Override
    public void onDestroy(Context context)
    {
        if (!initialized())
            return;
        mListening = false;
        mTalking = false;
        mRequestString = null;
        ASR.FREE_FORM.onDestroy(context);
        TTS.INSTANCE.onDestroy(context);
    }


    public UserSpeechPrompts setLanguage(Locale locale)
    {
        ASR.FREE_FORM.setLocale(locale);
        TTS.INSTANCE.setLocale(locale);
        return this;
    }


    public void prompt()
    {
        if (!initialized() || mTalking || mListening)
            return;
        mTalking = true;
        TTS.INSTANCE.say(mRequestString, new UtteranceProgressListener()
        {
            @Override
            public void onStart(String s)
            {

            }

            @Override
            public void onDone(String s)
            {
                startListening();
                mTalking = false;
            }

            @Override
            public void onError(String s)
            {
                mTalking = false;
            }
        });
    }


    private void startListening()
    {
        if (!initialized() || mListening)
            return;
        mListening = true;
        ASR.FREE_FORM.startListening(new RecognitionListener()
        {

            @Override
            public void onReadyForSpeech(Bundle bundle)
            {

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
            }

            @Override
            public void onError(int i)
            {
                mAnswerCandidates.clear();
                mListening = false;
            }

            @Override
            public void onResults(Bundle bundle)
            {
                mAnswerCandidates = ASR.FREE_FORM.getRecognizedSpeech(bundle);
                //TTS.INSTANCE.say("You said "+mAnswerCandidates.get(0));
                mListening = false;
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


    /**
     *
     * @return The last answer provided by the user, or null if he was never prompted or if the
     * answers were cleared
     */
    public String getLastAnswer()
    {
        return mAnswerCandidates.size() > 0? String.join(";", mAnswerCandidates) : null;
    }


    public void clearLastAnswer()
    {
        mAnswerCandidates.clear();
    }



}
