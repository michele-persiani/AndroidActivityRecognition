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


public class SensorAccumulator implements SensorEventListener
{
    ReentrantLock lock = new ReentrantLock();
    Function<SensorEvent, Boolean> state;

    protected Sensor sensor = null;        // These are initialized in the first call of state.apply() (by using uninitializedState)
    protected long startTimestamp = 0L;
    protected long lastTimestamp = 0L;
    protected DataFrame dataframe;
    protected Map<String, Function<SensorEvent, Object>> columnGetters;

    public SensorAccumulator()
    {
        this.state = unitializedState();
        this.dataframe = new DataFrame();
        columnGetters = new HashMap<>();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        state.apply(sensorEvent);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }


    public void reset()
    {
        state = unitializedState();
    }


    public int countReadings()
    {
        return dataframe.countRows();
    }


    public Sensor getSensor()
    {
        return sensor;
    }


    public DataFrame getDataFrame()
    {
        lock.lock();
        DataFrame df = dataframe.clone();
        lock.unlock();
        return df;
    }


    protected Map<String, Function<SensorEvent, Object>> columnGetters(int num_features)
    {
        HashMap<String, Function<SensorEvent, Object>> getters =  new HashMap<>();
        getters.put("timestamp", (e) -> e.timestamp / 1e6);
        getters.put("relative_timestamp", (e) -> (e.timestamp - startTimestamp) / 1e6);
        getters.put("delta_timestamp", (e) -> (e.timestamp - lastTimestamp) / 1e6);
        getters.put("accuracy", (e) -> e.accuracy);

        for (int i=0; i < num_features; i++)
        {
            @SuppressLint("DefaultLocale") String colname = String.format("f_%d", i);
            final int iFinal = i;
            getters.put(colname, (e) -> e.values[iFinal]);

        }

        return getters;
    }


    protected Function<SensorEvent, Boolean> unitializedState()
    {

        return event -> {
            columnGetters = columnGetters(event.values.length);

            String[] colNames = columnGetters.keySet().toArray(new String[0]);

            sensor = event.sensor;
            startTimestamp = lastTimestamp = event.timestamp;
            dataframe = new DataFrame(colNames);
            state = initializedState();
            onSensorChanged(event);
            return true;
        };

    }

    protected Function<SensorEvent, Boolean> initializedState()
    {

        return event -> {
            if (!event.sensor.getName().equals(sensor.getName()))
            {
                return false;
            }
            lock.lock();

            HashMap<String, Object> row = new HashMap<>();

            columnGetters.forEach((colname, getter) -> row.put(colname, getter.apply(event)));

            lastTimestamp = event.timestamp;
            dataframe.appendRow(row);
            lock.unlock();
            return true;
        };

    }
}
