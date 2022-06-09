package umu.software.activityrecognition.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

import umu.software.activityrecognition.chatbot.DialogflowChatBot;
import umu.software.activityrecognition.chatbot.SpeechChatbot;


/**
 * Bound service to access a dialogflow chatbot.
 * Requires Google Cloud API Key to function. These can be sent with the binding intents or after
 * through the initialize() method
 */
public class DialogflowService extends Service
{
    public static class Binder extends LocalBinder<DialogflowService>
    {
        public Binder(DialogflowService service)
        {
            super(service);
        }
    }

    public static final String EXTRA_VOICE_SPEED            = "EXTRA_VOICE_SPEED";
    public static final String EXTRA_LANGUAGE               = "EXTRA_LANGUAGE";

    public static final String DEFAULT_LANGUAGE             = "en";
    public static final float DEFAULT_VOICE_SPEED           = 1.f;

    public static String LOG_TAG                            = "DialogflowService";

    /** The Google Cloud API Key to use, in json format */
    public static final String EXTRA_DIALOGFLOW_KEY = "EXTRA_DIALOGFLOW_KEY";

    private SpeechChatbot mChatBot;
    private LocalBinder<DialogflowService> mBinder;

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mChatBot != null)
            mChatBot.disconnect((result) -> {
                Log.i(LOG_TAG, String.format("Chatbot disconnected (%s)", result));
            });
    }

    public void initialize(@NonNull Bundle extras)
    {
        String jsonKey = extras.getString(EXTRA_DIALOGFLOW_KEY);
        String language = extras.getString(EXTRA_LANGUAGE, DEFAULT_LANGUAGE);
        float voiceSpeed = extras.getFloat(EXTRA_VOICE_SPEED, DEFAULT_VOICE_SPEED);
        if (mChatBot != null)
            mChatBot.disconnect((r) -> {});

        mChatBot = new SpeechChatbot(
                new DialogflowChatBot(jsonKey),
                true
        );
        mChatBot.connect((result) -> {
            Log.i(LOG_TAG, String.format("Chatbot connected (%s)", result));
        });

        mChatBot.setVoicePersona(
                Locale.forLanguageTag(language),
                voiceSpeed
        );
    }

    public boolean isConnected()
    {
        return mChatBot != null;
    }


    public SpeechChatbot getChatBot()
    {
        return mChatBot;
    }


    public void perform(Consumer<SpeechChatbot> operation)
    {
        if (isConnected())
            operation.accept(mChatBot);
    }


    /**
     * The Google API Key can be set in the binding intent as an extra string EXTRA_DIALOGFLOW_KEY.
     * In this way the chatbot will be initialized the first time the service is bound
     * @param intent binding intent
     * @return the service's binder
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        if (!isConnected() && intent.hasExtra(EXTRA_DIALOGFLOW_KEY))
            initialize(intent.getExtras());

        if (mBinder == null)
            mBinder = new DialogflowService.Binder(this);
        return mBinder;
    }

    public static ServiceConnectionHandler<DialogflowService.Binder> getConnection(Context context, String apiKey, @Nullable String language, @Nullable Float voiceSpeed)
    {
        return new ServiceConnectionHandler<DialogflowService.Binder>(context)
                .setAutoRebind(true)
                .setIntentBuilder(intent -> {
                    intent.putExtra(EXTRA_DIALOGFLOW_KEY, apiKey);
                    if (language != null)   intent.putExtra(EXTRA_LANGUAGE, language);
                    if (voiceSpeed != null) intent.putExtra(EXTRA_VOICE_SPEED, voiceSpeed.floatValue());
                });
    }
}
