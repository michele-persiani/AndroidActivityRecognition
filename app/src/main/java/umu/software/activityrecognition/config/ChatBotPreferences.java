package umu.software.activityrecognition.config;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.preferences.PreferencesBuilder;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class ChatBotPreferences extends PreferencesModule
{
    /* Initialization values */
    public static final String DEFAULT_LANGUAGE                             = "en-US";
    public static final int DEFAULT_VOICE_SPEED                             = 90;
    public static final String DEFAULT_RECURRENT_EVENT_NAME                 = "ClassifyActivityShort";
    public static final boolean DEFAULT_SEND_RECURRENT_EVENT                = true;
    public static final String DEFAULT_SEND_RECURRENT_EVENT_EVERY_MINUTES   = "10";



    public ChatBotPreferences(Context context)
    {
        super(context);
    }


    public String getApiKey()
    {
        return AndroidUtils.readRawResourceFile(mContext, R.raw.dialogflow_credentials);
    }

    public String getLanguage()
    {
        return getString(getStringFromId(R.string.chatbot_language), DEFAULT_LANGUAGE);
    }

    
    public float getVoiceSpeed()
    {
        return getFloat(getStringFromId(R.string.chatbot_voice_speed), DEFAULT_VOICE_SPEED) / 100.f;
    }


    public boolean sendRecurrentEvent()
    {
        return getBoolean(R.string.chatbot_send_recurrent_event, DEFAULT_SEND_RECURRENT_EVENT);
    }


    public long recurrentEventMillis()
    {
        return TimeUnit.MILLISECONDS.convert(
                getInt(R.string.chatbot_recurrent_event_minutes, Integer.parseInt(DEFAULT_SEND_RECURRENT_EVENT_EVERY_MINUTES)),
                TimeUnit.MINUTES
        );
    }


    public String recurrentEventName()
    {
        return getString(R.string.chatbot_recurrent_event_name, DEFAULT_RECURRENT_EVENT_NAME);
    }

    @Override
    protected void initialize(Context context, PreferencesBuilder builder)
    {
        builder.initString(getStringFromId(R.string.chatbot_language), DEFAULT_LANGUAGE);
        builder.initInt(getStringFromId(R.string.chatbot_voice_speed), DEFAULT_VOICE_SPEED);
        builder.initBoolean(getStringFromId(R.string.chatbot_send_recurrent_event), DEFAULT_SEND_RECURRENT_EVENT);
        builder.initString(getStringFromId(R.string.chatbot_recurrent_event_minutes), DEFAULT_SEND_RECURRENT_EVENT_EVERY_MINUTES);
        builder.initString(getStringFromId(R.string.chatbot_recurrent_event_name), DEFAULT_RECURRENT_EVENT_NAME);
    }
}
