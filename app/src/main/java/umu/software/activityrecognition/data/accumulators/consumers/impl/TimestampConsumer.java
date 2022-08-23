package umu.software.activityrecognition.data.accumulators.consumers.impl;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public abstract class TimestampConsumer<T> implements BiConsumer<T, DataFrame.Row>
{
    private final String prefix;
    private long previousTimeMillis = 0L;

    protected TimestampConsumer(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    public void accept(T e, DataFrame.Row row)
    {
        long currentTime = currentTimeMillis(e);
        long deltaTime = currentTime - previousTimeMillis;
        previousTimeMillis += deltaTime;
        row.put(prefix + "_timestamp", currentTime);
        row.put(prefix + "_delta_timestamp", deltaTime);
    }

    protected abstract long currentTimeMillis(T e);


}
