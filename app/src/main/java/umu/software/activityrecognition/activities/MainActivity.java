package umu.software.activityrecognition.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.function.Function;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.SensorListAdapter;
import umu.software.activityrecognition.common.AndroidUtils;
import umu.software.activityrecognition.common.lifecycles.LifecyclesActivity;
import umu.software.activityrecognition.sensors.persistence.Persistence;
import umu.software.activityrecognition.sensors.RecordService;
import umu.software.activityrecognition.sensors.RecordServiceConnection;
import umu.software.activityrecognition.sensors.RecordServiceStarter;
import umu.software.activityrecognition.tflite.TFLiteModel;

/**
 * The Main Activity just start RecordServive and then finishes.
 */
public class MainActivity extends LifecyclesActivity
{


    RecordServiceConnection serviceConnection = new RecordServiceConnection();




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Persistence.INSTANCE.askPermissions(this);

        //addLifecycleElement(TFLiteModel.ENCODER_GRAVITY);


        setContentView(R.layout.activity_main);


        SensorManager sensorManager = AndroidUtils.getSensorManager(this);


        RecyclerView recyclerView = findViewById(R.id.sensorlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        SensorListAdapter adapter = new SensorListAdapter(sensorManager);
        recyclerView.setAdapter(adapter);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        RecordServiceStarter.broadcast(this, 10);


        //startTFLitePrediction();
        finish(); // Comment this line to see the available sensors in the GUI
    }

    /**
     * TO BE REMOVED Try TFLite components
     */
    void startTFLitePrediction()
    {


        new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true){
                if (TFLiteModel.ENCODER_GRAVITY.predict()) {
                    float[][] prediction = TFLiteModel.ENCODER_GRAVITY.getOutput(0);
                    Log.i("prediction", Arrays.toString(prediction[0]));
                }
            }
        }).start();
    }

    private void bindAndSaveFiles()
    {
        serviceConnection.setCallback(new Function<RecordService, Object>()
        {

            @SuppressLint("DefaultLocale")
            @Override
            public Object apply(RecordService recordService)
            {
                Integer[] result = new Integer[3];

                try {
                    result = recordService.saveZipClearSensorsFiles().call();
                } catch (Exception e) { e.printStackTrace();}

                int savedFiles   = result[0];
                int zippedFiles  = result[1];
                int deletedFiles = result[2];


                Toast.makeText(
                        MainActivity.this,
                        String.format("%d files saved, %d files zipped and %d files deleted.", savedFiles, zippedFiles, deletedFiles),
                        Toast.LENGTH_LONG)
                        .show();

                unbindService(serviceConnection);
                return null;
            }
        }).bind(this);
    }
}