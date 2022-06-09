package umu.software.activityrecognition.data.accumulators;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import com.google.common.collect.Queues;

import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Class that receives events and accumulates them into a DataFrame. The events are accumulated up to when
 * the reset() method is called, at which the dataframe is reset
 * initializeConsumers() is an abstract method that defines how events are transformed into dataframe rows
 * @param <T> the class of the received events
 */
public abstract class Accumulator<T> implements Consumer<T>, Supplier<DataFrame>
{

    protected DataFrame dataframe = new DataFrame();
    protected Queue<BiConsumer<T, DataFrame.Row>> eventConsumers = Queues.newConcurrentLinkedQueue();
    SupplierThread<T> supplier;
    private Integer windowSize;

    private long minDelayMillis = 0L;
    protected long lastTimestamp = 0L;


    public Accumulator()
    {
        initializeConsumers(eventConsumers);
        setWindowSize(null);
        setMinDelayMillis(0);
    }



    /**
     * Getter for the current system time in milliseconds of an event
     * @param event the event to find the time for
     * @return system time in milliseconds
     */
    protected long getCurrentTimeMillis(T event)
    {
        return System.currentTimeMillis();
    }


    /**
     * Set the minimum delay between an event and the successive. The accumulator won't process
     * events that come faster that this delay
     * @param minDelayMillis the new minum delay between events
     */
    public void setMinDelayMillis(long minDelayMillis)
    {
        this.minDelayMillis = Math.max(0, minDelayMillis);
        if (hasSupplier())
        {
            supplier.stop();
            supplier.start(this.minDelayMillis);
        }
    }


    /**
     * Start a supplier thread for this accumulator. The supplier continuosly supplies the accumulator
     * with events by using the provided callable.
     * @param supplier the function from which the supplier fetches the events
     */
    public void startSupplier(Supplier<T> supplier)
    {
        stopSupplier();
        this.supplier = new SupplierThread<T>(this, supplier);
        this.supplier.start(minDelayMillis);
    }


    /**
     * Stop the supply process
     */
    public void stopSupplier()
    {
        if (hasSupplier())
            supplier.stop();
        supplier = null;
    }


    /**
     * Returns whether the accumulator has a running supplier
     * @return whether the accumulator has a running supplier
     */
    public boolean hasSupplier()
    {
        return supplier != null;
    }


    /**
     * Getter for the queue of consumers
     * @return the queue of consumers, in the order in which they parse events
     */
    public Queue<BiConsumer<T, DataFrame.Row>> consumers()
    {
        return eventConsumers;
    }


    /**
     * Accepts an event to be processed (ie. accumulated in the dataframe).
     * @param event the event to accumulate
     */
    @Override
    public synchronized void accept(T event)
    {
        if(!filter(event))
            return;
        lastTimestamp = getCurrentTimeMillis(event);
        DataFrame.Row row = new DataFrame.Row();

        eventConsumers.forEach(c -> {
            c.accept(event, row);
        });
        dataframe.appendRow(row);
        if (windowSize != null)
            while (dataframe.countRows() > windowSize)
                dataframe.popFirstRow();
    }


    /**
     * Filter an event. Events not passing this filter are discarded
     * @param event input event to be filtered
     * @return whether the event should pass the filter.
     */
    protected boolean filter(T event)
    {
        return (getCurrentTimeMillis(event) - lastTimestamp) >= minDelayMillis;
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
    public synchronized void clearDataFrame()
    {
        dataframe = new DataFrame();
        dataframe.setName(getDataFrameName());
    }


    /**
     * Count dataframe rows
     * @return the number of accumulated rows
     */
    public synchronized int countReadings()
    {
        return dataframe.countRows();
    }


    /**
     * Get a cloned version of the accumulated dataframe
     * @return a cloned version of the accumulated dataframe
     */
    public synchronized DataFrame getDataFrame()
    {
        DataFrame df = dataframe.clone();
        df.setName(getDataFrameName());
        return df;
    }

    /**
     * See getDataFrame()
     * @return a cloned version of the accumulated dataframe
     */
    @Override
    public DataFrame get()
    {
        return getDataFrame();
    }

    /**
     * Getter for the name of the dataframe
     * @return the name of the produced DataFrame
     */
    protected abstract String getDataFrameName();


    /**
     * Get the event consumers that will populate a dataframe's row using the events.
     * Consumers can be further specified using the method consumers()
     * @param eventConsumers  the queue of consumers that will be used, in the given order, to transform an event into
     * a DataFrame.Row.
     */
    protected abstract void initializeConsumers(Queue<BiConsumer<T, DataFrame.Row>> eventConsumers);


    /**
     * Start the event recordings. Called by onStart()
     */
    protected abstract void startRecording();


    /**
     * Stop the event recordings. Called by onStop()
     */
    protected abstract void stopRecording();

}
