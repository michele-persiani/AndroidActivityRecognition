package umu.software.activityrecognition.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


import androidx.annotation.Nullable;

import java.util.List;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.permissions.Permissions;
import umu.software.activityrecognition.services.RecordServiceHelper;
import umu.software.activityrecognition.speech.UserPrompt;

public class MainActivity extends MenuActivity
{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Permissions.askPermissions(this);
    }

    @Override
    protected void buildMenu()
    {
        RecordServiceHelper serviceHelper = RecordServiceHelper.newInstance(this);
        addMenuEntry("Start record service", (e) -> {
            serviceHelper.startRecording(null);
            serviceHelper.startRecurrentSave();
        });
        addMenuEntry("Stop record service", (e) -> {
            serviceHelper.stopRecording();
            serviceHelper.stopRecurrentSave();
        });

        addMenuEntry("Save files", (e) -> {
            serviceHelper.saveZipClearFiles(null);
        });


        addMenuEntry("Available sensors", (e) -> {
            Intent intent = new Intent(this, SensorsActivity.class);
            startActivity(intent);
        });


        addMenuEntry("Prompt user", (e) -> {
            UserPrompt.INSTANCE.prompt(getString(R.string.request_user_classification), new UserPrompt.Callback()
            {
                @Override
                public void onStartSpeaking() { }

                @Override
                public void onSpeakingDone() { }

                @Override
                public void onStartListening() { }

                @Override
                public void onListeningDone() { }

                @Override
                public void onResult(List<String> answerCandidates)
                {
                    if (answerCandidates.size() == 0)
                        return;
                    String answer = answerCandidates.get(0);
                    serviceHelper.setSensorsLabel(answer);
                }

                @Override
                public void onError(int error)
                {
                    Toast.makeText(MainActivity.this,
                            String.format("Error during user prompt: %s", error),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });



        addMenuEntry("Launch Chatbot in English", (e) -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            intent.putExtra(ChatbotActivity.EXTRA_BOT_LANGUAGE, ChatbotActivity.BOT_LANGUAGE_ENGLISH);
            startActivity(intent);
        });


        addMenuEntry("Launch Chatbot in Swedish", (e) -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            intent.putExtra(ChatbotActivity.EXTRA_BOT_LANGUAGE, ChatbotActivity.BOT_LANGUAGE_SWEDISH);
            startActivity(intent);
        });
        addMenuEntry("Preferences", (e) -> {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        });
    }

}