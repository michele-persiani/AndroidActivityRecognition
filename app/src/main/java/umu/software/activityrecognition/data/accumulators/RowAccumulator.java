package umu.software.activityrecognition.data.accumulators;

import java.util.Queue;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.accumulators.consumers.EventConsumersFactory;
import umu.software.activityrecognition.data.dataframe.DataFrame;


/**
 * Accumulator that accepts rows to add directly to the dataframe
 */
public class RowAccumulator extends Accumulator<DataFrame.Row>
{
    private final String name;

    public RowAccumulator(String name)
    {
        this.name = name;
        setMinDelayMillis(0);
    }

    @Override
    protected String getDataFrameName()
    {
        return name;
    }

    @Override
    protected void initializeConsumers(Queue<BiConsumer<DataFrame.Row, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((source, target) -> target.putAll(source));
        eventConsumers.add(EventConsumersFactory.newEpochTimestamp());
    }

}
