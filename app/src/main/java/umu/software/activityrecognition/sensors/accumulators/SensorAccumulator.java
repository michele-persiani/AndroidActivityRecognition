package umu.software.activityrecognition.sensors.accumulators;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.c_bata.DataFrame;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;


public class SensorAccumulator extends Accumulator<SensorEvent> implements SensorEventListener
{
    protected Sensor sensor;
    protected long lastTimestamp = 0L;

    public SensorAccumulator()
    {
    }

    @Override
    public void reset()
    {
        super.reset();
        lastTimestamp = 0L;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        accept(sensorEvent);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }


    public Sensor getSensor()
    {
        return sensor;
    }

    @Override
    protected Function<SensorEvent, Boolean> unitializedState()
    {
        return super.unitializedState();
    }


    @SuppressLint("DefaultLocale")
    @Override
    protected List<BiConsumer<SensorEvent, DataFrame.Row>> initializeConsumers()
    {
        return Lists.newArrayList((e, r) -> {
            r.put("timestamp", TimeUnit.MILLISECONDS.convert(e.timestamp, TimeUnit.NANOSECONDS));
            r.put("delta_timestamp", TimeUnit.MILLISECONDS.convert(
                    lastTimestamp > 0? e.timestamp - lastTimestamp : 0,
                    TimeUnit.NANOSECONDS
            ));
            r.put("accuracy", e.accuracy);
            for (int i=0; i < e.values.length; i++)
            {
                String colname = String.format("f_%d", i);
                r.put(colname, e.values[i]);
            }
            lastTimestamp = e.timestamp;
            this.sensor = e.sensor;
        });

    }


}
