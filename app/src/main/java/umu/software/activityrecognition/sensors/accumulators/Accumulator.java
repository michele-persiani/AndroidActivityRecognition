package umu.software.activityrecognition.sensors.accumulators;

import android.annotation.SuppressLint;

import com.c_bata.DataFrame;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Accumulator<T> implements Consumer<T>
{
    private final ReentrantLock lock = new ReentrantLock();
    private Function<T, Boolean> state;

    protected DataFrame dataframe;
    protected List<BiConsumer<T, DataFrame.Row>> consumers;

    public Accumulator()
    {
        state = unitializedState();
        dataframe = new DataFrame();
    }

    public List<BiConsumer<T, DataFrame.Row>> consumers()
    {
        return consumers;
    }

    @Override
    public void accept(T event)
    {
        state.apply(event);
    }


    public void reset()
    {
        state = unitializedState();
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


    protected Function<T, Boolean> unitializedState()
    {

        return event -> {
            consumers = initializeConsumers();
            dataframe = new DataFrame();
            state = initializedState();
            accept(event);
            return true;
        };

    }

    protected Function<T, Boolean> initializedState()
    {
        return event -> {
            lock.lock();
            DataFrame.Row row = new DataFrame.Row();
            consumers.forEach(c -> c.accept(event, row));
            dataframe.appendRow(row);
            lock.unlock();
            return true;
        };
    }

}
