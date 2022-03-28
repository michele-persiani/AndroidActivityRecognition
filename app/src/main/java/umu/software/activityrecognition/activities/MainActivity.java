package umu.software.activityrecognition.activities;

import android.content.Intent;
import android.os.Bundle;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.sensors.persistence.Persistence;
import umu.software.activityrecognition.services.RecordService;
import umu.software.activityrecognition.services.RecordServiceConnection;
import umu.software.activityrecognition.speech.ASR;

/**
 * The Main Activity just start RecordServive and then finishes.
 */
public class MainActivity extends ButtonsActivity
{


    RecordServiceConnection serviceConnection = new RecordServiceConnection();



    @Override
    protected void buildButtons()
    {
        buildButton(R.id.button1, "Start record service", (e) -> {
            RecordService.start(this, 10);
        });
        buildButton(R.id.button2, "Stop record service", (e) -> {
            RecordService.stop(this);
        });

        buildButton(R.id.button3, "Available sensors", (e) -> {
            Intent intent = new Intent(this, SensorsActivity.class);
            startActivity(intent);
        });


        buildButton(R.id.button4, "Prompt user", (e) -> {
            RecordService.requestUserClassification(this);
            serviceConnection.setCallback((s) -> {
                s.requestUserClassification();
                unbindService(serviceConnection);
                return null;
            }).bind(this);
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Persistence.INSTANCE.askPermissions(this);
        ASR.FREE_FORM.askPermissions(this);

    }

    @Override
    protected void onStart()
    {
        super.onStart();


    }

}