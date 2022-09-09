package umu.software.activityrecognition.preferences;

import android.content.Context;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class SpeechServicePreferences extends PreferencesModule
{
    public SpeechServicePreferences(Context context)
    {
        super(context);
    }

    @Override
    protected void initialize()
    {
        maxRecognizedResults().init(
                getResources().getInteger(R.integer.speech_default_max_recognized_results)
        );
        language().init(
                getResources().getString(R.string.speech_default_language)
        );
        voiceSpeed().init(
                getResources().getInteger(R.integer.speech_default_voice_speed)
        );
    }

    public Preference<Integer> maxRecognizedResults()
    {
        return getInt(R.string.speech_max_recognized_results);
    }


    public Preference<String> language()
    {
        return getString(R.string.speech_language);
    }


    public Preference<Integer> voiceSpeed()
    {
        return getInt(R.string.speech_voice_speed);
    }


    public Preference<String> voiceName()
    {
        return getString(R.string.speech_voice_name);
    }
}
