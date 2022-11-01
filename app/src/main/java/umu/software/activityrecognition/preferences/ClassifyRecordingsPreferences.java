package umu.software.activityrecognition.preferences;

import android.content.Context;
import android.os.Environment;


import com.google.common.collect.Lists;

import java.nio.file.Paths;
import java.util.List;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.preferences.PreferencesInitializer;
import umu.software.activityrecognition.shared.preferences.PreferencesModule;

public class ClassifyRecordingsPreferences extends PreferencesModule
{
    static {
        PreferencesInitializer.addInitialization(ClassifyRecordingsPreferences.class);
    }


    public ClassifyRecordingsPreferences(Context context)
    {
        super(context);
    }


    @Override
    protected void initialize()
    {
        zipDestinationFolder().init(
                Paths.get(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                getResources().getString(R.string.application_documents_folder),
                        getResources().getString(R.string.application_labels_folder)
                ).toString()
        );
        silenceDbThreshold().init(getResources().getInteger(R.integer.speech_default_silence_db_threshold));
        minSilenceLengthSeconds().init(getResources().getInteger(R.integer.speech_default_min_silence_secs));
        questionsLanguage().init(getResources().getString(R.string.classify_questions_default_language));
        questions().init(
                Lists.newArrayList(getResources().getStringArray(R.array.classify_questions_default_questions))
        );

        askRecurrentQuestions().init(getResources().getBoolean(R.bool.ping_event_default_send_ping));
        recurrentQuestionsEveryMinutes().init(getResources().getInteger(R.integer.ping_event_default_ping_minutes));
        maxSpeechLengthSeconds().init(getResources().getInteger(R.integer.speech_default_max_length_secs));
        onSpeechVibrationLength().init(getResources().getInteger(R.integer.speech_default_on_speech_vibration_length_millis));
        onSpeechVibrationMinDbDelta().init(0.5f);
    }


    public Preference<String> zipDestinationFolder()
    {
        return getString(R.string.classify_zip_destination_folder);
    }


    public Preference<Integer> silenceDbThreshold()
    {
        return getInt(R.string.speech_silence_db_threshold);
    }


    public Preference<Integer> minSilenceLengthSeconds()
    {
        return getInt(R.string.speech_min_silence_secs);
    }


    public Preference<Integer> maxSpeechLengthSeconds()
    {
        return getInt(R.string.speech_max_length_secs);
    }


    public Preference<Integer> onSpeechVibrationLength()
    {
        return getInt(R.string.speech_on_speech_vibration_length_millis);
    }

    public Preference<Float> onSpeechVibrationMinDbDelta()
    {
        return getFloat(R.string.speech_on_speech_vibration_min_db_delta);
    }


    public Preference<String> questionsLanguage()
    {
        return getString(R.string.classify_questions_default_language);
    }

    public Preference<List<String>> questions()
    {
        return getStringList(R.string.classify_questions_questions);
    }


    public Preference<Boolean> askRecurrentQuestions()
    {
        return getBoolean(R.string.ping_event_send_recurrent_event);
    }


    public Preference<Integer> recurrentQuestionsEveryMinutes()
    {
        return getInt(R.string.ping_event_recurrent_event_minutes);
    }
}
