package umu.software.activityrecognition.activities;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.views.SensorListAdapter;



import umu.software.activityrecognition.shared.permissions.Permissions;

public class SensorsActivity extends Activity
{


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_listview);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Ask permissions
        Permissions.ACTIVITY_RECOGNITION.askPermission(this);
        Permissions.BODY_SENSORS.askPermission(this);

        // Setup GUI
        SensorManager sensorManager = AndroidUtils.getSensorManager(this);
        SensorListAdapter adapter = new SensorListAdapter(sensorManager);
        RecyclerView recyclerView = findViewById(R.id.listview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

}
