package umu.software.activityrecognition.activities;

import android.content.Intent;
import android.os.Bundle;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.services.RecordService;
import umu.software.activityrecognition.services.RecordServiceConnection;


public class MainActivity extends ButtonsActivity
{


    RecordServiceConnection serviceConnection = new RecordServiceConnection();



    @Override
    protected void buildButtons()
    {
        buildButton(R.id.button1, "Start record service", (e) -> {
            RecordService.startRecording(
                    this,
                    RecordService.DEFAULT_RECURRENT_SAVE_SECS,
                    RecordService.DEFAULT_MIN_DELAY_MILLIS,
                    RecordService.DEFAULT_RECORDED_SENSOR_TYPES);
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

        buildButton(R.id.button5, "Save files", (e) -> {
            RecordService.saveZipClearFiles(this);
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        RecordService.askPermissions(this);
    }


}