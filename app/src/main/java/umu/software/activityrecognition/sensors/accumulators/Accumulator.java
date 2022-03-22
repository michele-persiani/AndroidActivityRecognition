package umu.software.activityrecognition.sensors.accumulators;

import android.annotation.SuppressLint;

import com.c_bata.DataFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Accumulator<T> implements Consumer<T>
{
    ReentrantLock lock = new ReentrantLock();
    Function<T, Boolean> state;

    protected DataFrame dataframe;
    protected Map<String, Function<T, Object>> columnGetters;

    public Accumulator()
    {
        state = unitializedState();
        dataframe = new DataFrame();
        columnGetters = new HashMap<>();
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

    protected abstract Map<String, Function<T, Object>> initializeColumns(T event);



    protected Function<T, Boolean> unitializedState()
    {

        return event -> {
            columnGetters = initializeColumns(event);

            String[] colNames = columnGetters.keySet().toArray(new String[0]);

            dataframe = new DataFrame(colNames);
            state = initializedState();
            accept(event);
            return true;
        };

    }

    protected Function<T, Boolean> initializedState()
    {
        return event -> {
            lock.lock();

            HashMap<String, Object> row = new HashMap<>();

            columnGetters.forEach((colname, getter) -> row.put(colname, getter.apply(event)));

            dataframe.appendRow(row);
            lock.unlock();
            return true;
        };
    }

}
