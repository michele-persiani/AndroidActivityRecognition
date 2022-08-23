package umu.software.activityrecognition.data.accumulators;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import umu.software.activityrecognition.data.dataframe.DataFrame;

/**
 * Class managing a collection of accumulators. Supports a Map interface to add/remove accumulators
 * and provides, through getLifecycle() a LifecycleRegistry that can be utilized to start/stop the
 * accumulators all-together. Accumulators start/stop recording with ON_START/ON_STOP events
 */
public class AccumulatorsLifecycle implements LifecycleOwner, Map<Object, Accumulator<?>>
{
    Map<Object, Accumulator<?>> accumulators = Maps.newHashMap();
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
    public void putAll(@NonNull Map<?, ? extends Accumulator<?>> map)
    {
        for (Entry<?, ? extends Accumulator<?>> e : map.entrySet())
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
    public Collection<Accumulator<?>> values()
    {
        return Sets.newHashSet(accumulators.values());
    }

    @NonNull
    @Override
    public Set<Entry<Object, Accumulator<?>>> entrySet()
    {
        return Sets.newHashSet(accumulators.entrySet());
    }


    /**
     * Add an accumulator to those being managed
     * @param key key to identify the accumulator
     * @param accumulator accumulator to add
     * @return the accumulator previously stored with the given key, if any
     */
    public Accumulator<?> put(Object key, Accumulator<?> accumulator)
    {
        Accumulator<?> previous = null;
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
                    accumulator.stopSupplier();
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
     * Returns the accumulator with the associated key or null
     * @param key key to identify the accumulator
     * @return the accumulator with the associated key or null
     */
    @Nullable
    public Accumulator<?> get(Object key)
    {
        if (!containsKey(key))
            return null;
        return accumulators.get(key);
    }

    /**
     * Removes the accumulator from those being managed
     * @param key key to identify the accumulator
     * @return the accumulator being removed or null
     */
    @Nullable
    public Accumulator<?> remove(Object key)
    {
        if (!containsKey(key))
            return null;
        lifecycle.removeObserver(Objects.requireNonNull(observers.get(key)));
        LifecycleEventObserver obs = observers.remove(key);
        Objects.requireNonNull(obs)
                .onStateChanged(
                        this,
                        Objects.requireNonNull(Lifecycle.Event.downTo(Lifecycle.State.DESTROYED))
                );
        return accumulators.remove(key);
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
        List<DataFrame> dataframes = Lists.newArrayList();
        for (Accumulator<?> accum : accumulators.values())
        {
            DataFrame df = accum.getDataFrame();
            dataframes.add(df);
        }
        return dataframes;
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
        for (Accumulator<?> acc : accumulators.values())
            acc.clearDataFrame();
        return accumulators.size();
    }
}
