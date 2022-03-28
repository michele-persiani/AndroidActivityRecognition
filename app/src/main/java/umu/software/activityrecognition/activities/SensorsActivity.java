package umu.software.activityrecognition.activities;

import android.app.Activity;
import android.os.Bundle;
import android.hardware.SensorManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.SensorListAdapter;
import umu.software.activityrecognition.common.AndroidUtils;

/**
 * The Main Activity just start RecordServive and then finishes.
 */
public class SensorsActivity extends Activity
{




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SensorManager sensorManager = AndroidUtils.getSensorManager(this);

        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = findViewById(R.id.sensorlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SensorListAdapter adapter = new SensorListAdapter(sensorManager);
        recyclerView.setAdapter(adapter);
    }


}