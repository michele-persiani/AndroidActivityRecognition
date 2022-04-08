package umu.software.activityrecognition.activities;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.views.SensorListAdapter;


import android.hardware.Sensor;
import android.support.wearable.activity.WearableActivity;

import umu.software.activityrecognition.common.permissions.Permissions;
import umu.software.activityrecognition.data.accumulators.SensorAccumulators;
import umu.software.activityrecognition.data.accumulators.SensorAccumulatorManager;
import umu.software.activityrecognition.services.RecordService;

public class SensorsActivity extends WearableActivity
{

    SensorAccumulatorManager accumManager = SensorAccumulators.newAccumulatorManager(
            SensorAccumulators.newFactory(),
            false,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        accumManager.onCreate(this);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Ask permissions
        RecordService.askPermissions(this);
        Permissions.ACTIVITY_RECOGNITION.askPermission(this);
        Permissions.BODY_SENSORS.askPermission(this);

        // Setup GUI
        accumManager.onStart(this);

        SensorListAdapter adapter = new SensorListAdapter(accumManager);
        RecyclerView recyclerView = findViewById(R.id.sensorlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Start record service
        RecordService.startRecording(this,
                RecordService.DEFAULT_RECURRENT_SAVE_SECS,
                RecordService.DEFAULT_MIN_DELAY_MILLIS,
                RecordService.DEFAULT_RECORDED_SENSOR_TYPES
        );
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        accumManager.onStop(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        accumManager.onDestroy(this);
    }
}
