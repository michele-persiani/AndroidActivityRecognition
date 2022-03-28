package umu.software.activityrecognition.sensors.accumulators;

import android.annotation.SuppressLint;

import com.c_bata.DataFrame;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Accumulator<T> implements Consumer<T>
{
    private final ReentrantLock lock = new ReentrantLock();

    protected DataFrame dataframe;
    protected Queue<BiConsumer<T, DataFrame.Row>> consumers;

    public Accumulator()
    {
        dataframe = new DataFrame();
        consumers  = new ConcurrentLinkedQueue<>(initializeConsumers());
    }

    public Queue<BiConsumer<T, DataFrame.Row>> consumers()
    {
        return consumers;
    }

    @Override
    public void accept(T event)
    {
        lock.lock();
        DataFrame.Row row = new DataFrame.Row();
        consumers.forEach(c -> c.accept(event, row));

        dataframe.appendRow(row);
        lock.unlock();
    }


    public void reset()
    {
        lock.lock();
        dataframe = new DataFrame();
        lock.unlock();
    }


    public int countReadings()
    {
        return dataframe.countRows();
    }


    public DataFrame getDataFrame()
    {
        lock.lock();
        DataFrame df = dataframe.clone();
        lock.unlock();
        return df;
    }


    protected abstract List<BiConsumer<T, DataFrame.Row>> initializeConsumers();


}
