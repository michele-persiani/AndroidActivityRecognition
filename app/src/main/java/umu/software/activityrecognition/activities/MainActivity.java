package umu.software.activityrecognition.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import umu.software.activityrecognition.shared.permissions.Permissions;
import umu.software.activityrecognition.services.recordings.RecordServiceHelper;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.BaseSpeechRecognitionListener;

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



        addMenuEntry("Preferences", (e) -> {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        });


        addMenuEntry("Start record service", (e) -> {
            serviceHelper.startRecording(null);
            serviceHelper.startRecurrentSave();
        });


        addMenuEntry("Stop record service", (e) -> {
            serviceHelper.stopRecording();
            serviceHelper.stopRecurrentSave();
        });


        addMenuEntry("Save files", (e) -> {
            serviceHelper.saveZipClearFiles();
        });


        addMenuEntry("Available sensors", (e) -> {
            Intent intent = new Intent(this, SensorsActivity.class);
            startActivity(intent);
        });


        addMenuEntry("Set sensors label", (e) -> {

            ASR.FREE_FORM.startListening(new BaseSpeechRecognitionListener() {
                @Override
                protected void onRecognizedSpeech(List<Pair<String, Float>> results)
                {
                    super.onRecognizedSpeech(results);
                    String answer = results.get(0).first;
                    serviceHelper.setSensorsLabel(answer);
                    Toast.makeText(MainActivity.this,
                            String.format("Sensor label set to: (%s)", answer),
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onError(int i)
                {
                    super.onError(i);
                    Toast.makeText(MainActivity.this,
                            String.format("Error during user prompt: %s", i),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

        });


        addMenuEntry("Launch Chatbot", (e) -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected Button makeButton()
    {
        MaterialButton button = new MaterialButton(this);
        button.setMinHeight(150);
        button.setTextSize(20);
        button.setAllCaps(true);
        button.setCornerRadius(40);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        );
        return button;
    }
}