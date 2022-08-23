package umu.software.activityrecognition.preferences;

import android.content.Context;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class DialogflowServicePreferences extends PreferencesModule
{

    public DialogflowServicePreferences(Context context)
    {
        super(context);
    }


    @Override
    protected void initialize()
    {
        language().init(
                getResources().getString(R.string.chatbot_default_language)
        );
        voiceSpeed().init(
                getResources().getInteger(R.integer.chatbot_default_voice_speed)
        );
        apiKey().init(
                AndroidUtils.readRawResourceFile(mContext, R.raw.dialogflow_credentials)
        );
    }


    public Preference<String> apiKey()
    {
        return getString(R.string.chatbot_api_key);
    }


    public Preference<String> language()
    {
        return getString(R.string.chatbot_language);
    }

    
    public Preference<Integer> voiceSpeed()
    {
        return getInt(R.string.chatbot_voice_speed);
    }

    public Preference<String> voiceName()
    {
        return getString(R.string.chatbot_voice_name);
    }


}
