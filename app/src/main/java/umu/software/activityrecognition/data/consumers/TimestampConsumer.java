package umu.software.activityrecognition.data.consumers;

import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;

public abstract class TimestampConsumer implements Consumer<DataFrame.Row>
{
    private final String prefix;
    private long previousTimeMillis = 0L;


    protected TimestampConsumer(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    public void accept(DataFrame.Row row)
    {
        long currentTime = currentTimeMillis();
        long deltaTime = currentTime - previousTimeMillis;
        previousTimeMillis += deltaTime;
        row.put(prefix + "_timestamp", currentTime);
        row.put(prefix + "_delta_timestamp", deltaTime);
    }

    protected abstract long currentTimeMillis();


}
