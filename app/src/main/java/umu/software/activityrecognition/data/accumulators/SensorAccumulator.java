package umu.software.activityrecognition.data.accumulators;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.c_bata.DataFrame;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


public class SensorAccumulator extends Accumulator<SensorEvent> implements SensorEventListener
{
    protected long minDelayMillis;
    protected Sensor sensor;
    protected long lastTimestamp = 0L;

    public SensorAccumulator(long minDelayMillis)
    {
        this.minDelayMillis = minDelayMillis;
    }

    public SensorAccumulator()
    {
        this.minDelayMillis = 0;
    }


    public void setMinDelayMillis(long minDelayMillis)
    {
        this.minDelayMillis = Math.max(0, minDelayMillis);
    }

    @Override
    public void clearDataFrame()
    {
        super.clearDataFrame();
        lastTimestamp = 0L;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        accept(event);
    }

    @Override
    protected boolean filter(SensorEvent event)
    {
        return super.filter(event) && (event.timestamp - lastTimestamp) >= minDelayMillis;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }


    public Sensor getSensor()
    {
        return sensor;
    }


    @SuppressLint("DefaultLocale")
    @Override
    protected void initializeConsumers(Queue<BiConsumer<SensorEvent, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((e, r) -> {
            r.put("timestamp", TimeUnit.MILLISECONDS.convert(e.timestamp, TimeUnit.NANOSECONDS));
            r.put("delta_timestamp", TimeUnit.MILLISECONDS.convert(lastTimestamp > 0 ? e.timestamp - lastTimestamp : 0, TimeUnit.NANOSECONDS));
            r.put("accuracy", e.accuracy);
            for (int i = 0; i < e.values.length; i++) {
                String colname = String.format("f_%s", i);
                r.put(colname, e.values[i]);
            }
            lastTimestamp = e.timestamp;
            this.sensor = e.sensor;
        });
    }

}
