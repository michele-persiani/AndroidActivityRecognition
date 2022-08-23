package umu.software.activityrecognition.data.accumulators;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.data.accumulators.consumers.EventConsumersFactory;
import umu.software.activityrecognition.data.dataframe.DataFrame;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


public class SensorAccumulator extends Accumulator<SensorEvent> implements SensorEventListener
{
    private final SensorManager mSensorManager;
    protected Sensor mSensor;

    private Handler mHandler;


    public SensorAccumulator(SensorManager sensorManager, Sensor sensor)
    {
        mSensorManager = sensorManager;
        mSensor = sensor;
    }



    @Override
    protected String getDataFrameName()
    {
        return mSensor.getName().replace(" ", "_");
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        accept(event);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }

    @Override
    public long getCurrentTimeMillis(SensorEvent event)
    {
        return TimeUnit.MILLISECONDS.convert(event.timestamp, TimeUnit.NANOSECONDS);
    }


    public Sensor getSensor()
    {
        return mSensor;
    }


    @SuppressLint("DefaultLocale")
    @Override
    protected void initializeConsumers(Queue<BiConsumer<SensorEvent, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((e, r) -> {
            r.put("accuracy", e.accuracy);
            for (int i = 0; i < e.values.length; i++) {
                String colname = String.format("f_%s", i);
                r.put(colname, e.values[i]);
            }
        });
        eventConsumers.add(EventConsumersFactory.newSensorTimestamp());
        eventConsumers.add(EventConsumersFactory.newEpochTimestamp());
    }

    @Override
    public void startRecording()
    {
        super.startRecording();
        mHandler = AndroidUtils.newHandler();
        mSensorManager.registerListener(
                this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                mHandler
        );
    }

    @Override
    public void stopRecording()
    {
        super.stopRecording();
        mSensorManager.unregisterListener(this);
        mHandler.getLooper().quitSafely();
    }
}
