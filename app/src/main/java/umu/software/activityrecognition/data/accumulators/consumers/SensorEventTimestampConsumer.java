package umu.software.activityrecognition.data.accumulators.consumers;

import android.hardware.SensorEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public class SensorEventTimestampConsumer implements BiConsumer<SensorEvent, DataFrame.Row>
{
    private long previousTimeMillis = 0L;

    @Override
    public void accept(SensorEvent o, DataFrame.Row row)
    {
        long currentTime = TimeUnit.MILLISECONDS.convert(o.timestamp, TimeUnit.NANOSECONDS);
        long deltaTime = currentTime - previousTimeMillis;
        previousTimeMillis += deltaTime;
        row.put("timestamp", currentTime);
        row.put("delta_timestamp", deltaTime);
    }


}
