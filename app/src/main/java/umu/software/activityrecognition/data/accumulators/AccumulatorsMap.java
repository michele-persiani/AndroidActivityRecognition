package umu.software.activityrecognition.data.accumulators;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import umu.software.activityrecognition.data.dataframe.DataFrame;


/**
 * Class managing a collection of data accumulators. Supports a Map interface to add/remove accumulators
 * and provides, through getLifecycle() a LifecycleRegistry that can be utilized to start/stop the
 * accumulators all-together. Accumulators start/stop recording with ON_START/ON_STOP events
 */
public class AccumulatorsMap implements LifecycleOwner, Map<Object, DataAccumulator>
{
    Map<Object, DataAccumulator> accumulators = Maps.newHashMap();
    Map<Object, LifecycleEventObserver> observers = Maps.newHashMap();

    private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

    /**
     * LifecycleRegistry that controls the start and stopping of accumulators through ON_START/ON_STOP events
     * @return LifecycleRegistry that controls the start and stopping of accumulators
     */
    @NonNull
    @Override
    public LifecycleRegistry getLifecycle()
    {
        return lifecycle;
    }



    /**
     * Add an accumulator to those being managed
     * @param key key to identify the accumulator
     * @param accumulator accumulator to add
     * @return the accumulator previously stored with the given key, if any
     */
    public DataAccumulator put(Object key, DataAccumulator accumulator)
    {
        DataAccumulator previous = null;
        if (containsKey(key))
            previous = remove(key);

        LifecycleEventObserver obs = (source, event) -> {
            switch (event)
            {
                case ON_START:
                    accumulator.startRecording();
                    break;
                case ON_STOP:
                case ON_DESTROY:
                    accumulator.stopRecording();
                    break;
            }
        };


        observers.put(key, obs);
        accumulators.put(key, accumulator);
        lifecycle.addObserver(obs);
        return previous;
    }


    /**
     * Removes the accumulator from those being managed
     * @param key key to identify the accumulator
     * @return the accumulator being removed or null
     */
    @Nullable
    public DataAccumulator remove(Object key)
    {
        if (!containsKey(key))
            return null;
        lifecycle.removeObserver(observers.get(key));
        LifecycleEventObserver obs = observers.remove(key);
        obs.onStateChanged(
                this,
                Lifecycle.Event.downTo(Lifecycle.State.DESTROYED)
        );
        return accumulators.remove(key);
    }


    @Override
    public int size()
    {
        return accumulators.size();
    }

    @Override
    public boolean isEmpty()
    {
        return accumulators.isEmpty();
    }


    public boolean containsKey(Object key)
    {
        boolean result = accumulators.containsKey(key);
        assert !result || observers.containsKey(key);
        return result;
    }

    @Override
    public boolean containsValue(@Nullable Object o)
    {
        return accumulators.containsValue(o);
    }


    @Override
    public void putAll(@NonNull Map<?, ? extends DataAccumulator> map)
    {
        for (Entry<?, ? extends DataAccumulator> e : map.entrySet())
            put(e.getKey(), e.getValue());
    }


    @NonNull
    @Override
    public Set<Object> keySet()
    {
        return Sets.newHashSet(accumulators.keySet());
    }

    @NonNull
    @Override
    public Collection<DataAccumulator> values()
    {
        return Sets.newHashSet(accumulators.values());
    }

    @NonNull
    @Override
    public Set<Entry<Object, DataAccumulator>> entrySet()
    {
        return Sets.newHashSet(accumulators.entrySet());
    }


    /**
     * Returns the accumulator with the associated key or null
     * @param key key to identify the accumulator
     * @return the accumulator with the associated key or null
     */
    @Nullable
    public DataAccumulator get(Object key)
    {
        if (!containsKey(key))
            return null;
        return accumulators.get(key);
    }



    /**
     * Removes all accumulators
     */
    public void clear()
    {
        clearDataFrames();
        for (Object key : Lists.newArrayList(accumulators.keySet()))
            remove(key);
    }

    /**
     * Get all accumulated dataframes
     * @return list of dataframes, one for each accumulator
     */
    public List<DataFrame> getDataFrames()
    {
        return accumulators.values().stream().map(DataAccumulator::getDataFrame).collect(Collectors.toList());
    }

    /**
     * Return the DataFrame accumulated by the Accumulator identified through the key
     * @param key key to identify the accumulator
     * @return the DataFrame accumulated by the Accumulator identified through the key
     */
    public DataFrame getDataFrame(Object key)
    {
        if (!containsKey(key))
            return null;
        return get(key).getDataFrame();
    }

    /**
     * Resets all accumulators' dataframes
     * @return number of reset accumulators
     */
    public int clearDataFrames()
    {
        for (DataAccumulator acc : accumulators.values())
            acc.clearDataFrame();
        return accumulators.size();
    }
}
