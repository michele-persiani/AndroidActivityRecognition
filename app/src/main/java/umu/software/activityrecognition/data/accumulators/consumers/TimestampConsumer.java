package umu.software.activityrecognition.data.accumulators.consumers;

import android.os.SystemClock;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public class TimestampConsumer<T> implements BiConsumer<T, DataFrame.Row>
{
    private long previousTimeMillis = 0L;

    TimestampConsumer()
    {
        reset();
    }

    @Override
    public void accept(T o, DataFrame.Row row)
    {
        long currentTime = currentTimeMillis();
        long deltaTime = currentTime - previousTimeMillis;
        previousTimeMillis += deltaTime;
        row.put("timestamp", currentTime);
        row.put("delta_timestamp", deltaTime);
    }

    protected long currentTimeMillis()
    {
        return (long) (SystemClock.elapsedRealtimeNanos() * 1e-6);
    }

    public void reset()
    {
        previousTimeMillis = currentTimeMillis();
    }

}
