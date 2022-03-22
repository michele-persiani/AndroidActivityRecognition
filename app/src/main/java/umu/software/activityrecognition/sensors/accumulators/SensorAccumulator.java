package umu.software.activityrecognition.sensors.accumulators;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.c_bata.DataFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


public class SensorAccumulator extends Accumulator<SensorEvent> implements SensorEventListener
{
    protected Sensor sensor;
    protected long startTimestamp = 0L;
    protected long lastTimestamp = 0L;
    protected Map<String, Function<SensorEvent, Object>> columnGetters;

    public SensorAccumulator()
    {
        columnGetters = new HashMap<>();
    }

    @Override
    public void reset()
    {
        super.reset();
        startTimestamp = 0L;
        lastTimestamp = 0L;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        accept(sensorEvent);
        lastTimestamp = sensorEvent.timestamp;
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

    @Override
    protected Map<String, Function<SensorEvent, Object>> initializeColumns(SensorEvent event)
    {
        sensor = event.sensor;
        startTimestamp = lastTimestamp = event.timestamp;

        HashMap<String, Function<SensorEvent, Object>> getters =  new HashMap<>();
        getters.put("timestamp", (e) -> e.timestamp / 1e6);
        getters.put("relative_timestamp", (e) -> (e.timestamp - startTimestamp) / 1e6);
        getters.put("delta_timestamp", (e) -> (e.timestamp - lastTimestamp) / 1e6);
        getters.put("accuracy", (e) -> e.accuracy);

        for (int i=0; i < event.values.length; i++)
        {
            @SuppressLint("DefaultLocale") String colname = String.format("f_%d", i);
            final int iFinal = i;
            getters.put(colname, (e) -> e.values[iFinal]);

        }

        return getters;
    }



}
