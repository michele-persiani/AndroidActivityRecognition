package umu.software.activityrecognition.activities;


import android.app.Activity;
import android.os.Bundle;


import umu.software.activityrecognition.services.RecordService;

public class StartRecordServiceActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_start_record_service);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        RecordService.startRecording(this,
                RecordService.DEFAULT_RECURRENT_SAVE_SECS,
                RecordService.DEFAULT_MIN_DELAY_MILLIS,
                RecordService.DEFAULT_RECORDED_SENSOR_TYPES
        );
        finish();
    }
}