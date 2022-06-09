package umu.software.activityrecognition.data.accumulators;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.data.accumulators.consumers.ConsumersFactory;
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

    /**
     * Returns the sensor being recorded
     * @return the sensor being recorded
     */
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
        eventConsumers.add(ConsumersFactory.newSensorTimestamp());
    }

    @Override
    protected void startRecording()
    {
        mHandler = AndroidUtils.newHandler();
        mSensorManager.registerListener(
                this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL,
                mHandler
        );
    }

    @Override
    protected void stopRecording()
    {
        mSensorManager.unregisterListener(this);
        mHandler.getLooper().quitSafely();
    }
}
