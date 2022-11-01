package umu.software.activityrecognition.data.suppliers.impl;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataSupplier;

public class SensorEventSupplier implements DataSupplier, SensorEventListener
{

    private final SensorManager sensorManager;
    private final Sensor sensor;
    private SensorEvent lastReceivedEvent;

    public SensorEventSupplier(SensorManager sensorManager, Sensor sensor)
    {
        this.sensorManager = sensorManager;
        this.sensor = sensor;
    }

    @Override
    public String getName()
    {
        return sensor.getName();
    }

    @Override
    public void initialize()
    {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean isReady()
    {
        return lastReceivedEvent != null;
    }

    @Override
    public void dispose()
    {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void accept(DataFrame.Row r)
    {
        if(lastReceivedEvent == null)
            return;

        r.put("accuracy", lastReceivedEvent.accuracy);
        r.put("sensor_name", lastReceivedEvent.sensor.getName());
        r.put("sensor_event_timestamp", lastReceivedEvent.timestamp);

        for (int i = 0; i < lastReceivedEvent.values.length; i++)
        {
            String colname = String.format("f_%s", i);
            r.put(colname, lastReceivedEvent.values[i]);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        lastReceivedEvent = sensorEvent;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }

}
