package umu.software.activityrecognition.preferences;

import android.content.Context;

import java.util.Locale;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesInitializer;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class SpeechServicePreferences extends PreferencesModule
{

    static {
        PreferencesInitializer.addInitialization(SpeechServicePreferences.class);
    }

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
        minSilenceMillis().init(getResources().getInteger(R.integer.speech_default_min_silence_secs));
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


    public Preference<String> voiceName(Locale locale)
    {
        return getString(voiceKey(locale));
    }


    public Preference<Integer> minSilenceMillis()
    {
        return getInt(R.string.speech_min_silence_secs);
    }


    public String voiceKey(Locale locale)
    {
        return String.format("%s-%s", locale.getLanguage(), getString(R.string.speech_voice_name));
    }


}
