package umu.software.activityrecognition.data.accumulators;


import com.c_bata.DataFrame;
import com.google.common.collect.Queues;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import umu.software.activityrecognition.common.FunctionLock;

/**
 * Class that receives events and accumulates them into a DataFrame. The events are accumulated up to when
 * the reset() method is called, at which the dataframe is reset
 * initializeConsumers() is an abstract method that defines how events are transformed into dataframe rows
 * @param <T> the class of the received events
 */
public abstract class Accumulator<T> implements Consumer<T>
{
    protected final FunctionLock lock = FunctionLock.make();

    protected DataFrame dataframe = new DataFrame();
    protected Queue<BiConsumer<T, DataFrame.Row>> eventConsumers = Queues.newConcurrentLinkedQueue();
    AccumulatorSupplier<T> supplier;
    private Integer windowSize;


    public Accumulator()
    {
        initializeConsumers(eventConsumers);
        setWindowSize(null);
    }

    /**
     * Start a supplier thread for this accumulator.
     * @param supplier the supplier from which to fetch the events
     * @param delayMillis delay in between each event fetch
     */
    public void startSupplier(Callable<T> supplier, long delayMillis)
    {
        stopSupplier();
        this.supplier = new AccumulatorSupplier<>(this, supplier);
        this.supplier.start(delayMillis);
    }

    /**
     * Stop the supply process
     */
    public void stopSupplier()
    {
        if (supplier != null)
            supplier.stop();
    }

    /**
     * Getter for the queue of consumers
     * @return the queue of consumers, in the order in which they parse events
     */
    public Queue<BiConsumer<T, DataFrame.Row>> consumers()
    {
        return eventConsumers;
    }

    @Override
    public void accept(T event)
    {
        lock.withLock(() -> {
            if(!filter(event))
                return;
            DataFrame.Row row = new DataFrame.Row();
            eventConsumers.forEach(c -> c.accept(event, row));
            dataframe.appendRow(row);
            if (windowSize != null)
                while (dataframe.countRows() > windowSize)
                    dataframe.popFirstRow();
        });
    }

    /**
     * Filter an event
     * @param event input event to be filtered
     * @return whether the event should pass the filter.
     */
    protected boolean filter(T event)
    {
        return true;
    }

    /**
     * The window size is the maximum number of rows in the dataframe. If set to null it will be ignored
     * @param windowSize window size or null
     */
    public void setWindowSize(Integer windowSize)
    {
        assert windowSize == null || windowSize > 0;
        this.windowSize = windowSize;
    }


    /**
     * Resets the dataframe, clearing all of its rows
     */
    public void clearDataFrame()
    {
        lock.withLock(() -> {
            dataframe = new DataFrame();
        });
    }


    /**
     * Count dataframe rows
     * @return the number of accumulated rows
     */
    public int countReadings()
    {
        return lock.withLock(() -> dataframe.countRows());
    }

    /**
     * Get a cloned version of the accumulated dataframe
     * @return a cloned version of the accumulated dataframe
     */
    public DataFrame getDataFrame()
    {
        return lock.withLock(() -> dataframe.clone());
    }


    /**
     * Get the event consumers
     * @return the queue of consumers that will be used, in the given order, to transform an event into
     * a DataFrame.Row. Consumers can be further specified using the method consumers()
     */
    protected abstract void initializeConsumers(Queue<BiConsumer<T, DataFrame.Row>> eventConsumers);


}
