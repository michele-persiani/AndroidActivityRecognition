package umu.software.activityrecognition.shared.preferences;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;



public abstract class Preference<T>
{
    private final SharedPreferences preferences;
    private final String key;

    private final Map<Consumer<Preference<T>>, SharedPreferences.OnSharedPreferenceChangeListener> listeners = Maps.newHashMap();


    public Preference(SharedPreferences preferences, String key)
    {
        this.preferences = preferences;
        this.key = key;
    }

    /**
     * Returns the value of this preference, or 'defaultValue' if not set
     * @param defaultValue the value to return if the property is not set
     * @return the value of this preference, or 'defaultValue' if not set
     */
    public T get(@Nullable T defaultValue)
    {
        return getValue(preferences, key, defaultValue);
    }


    /**
     * Returns the value of this preference, or null if not set
     * @return the value of this preference, or null if not set
     */
    public T get()
    {
        return get(null);
    }


    /**
     * Checks whether the preference has a value set
     * @return whether the preference has a value set
     */
    public boolean isSet()
    {
        return preferences.contains(key);
    }

    /**
     * Sets the value for this preference, or remove it if 'value' is null
     * @param value the value to set or null
     */
    public void set(@Nullable T value)
    {

        SharedPreferences.Editor editor = preferences.edit();
        if (value == null )
            editor.remove(key);
        else
            setValue(editor, key, value);
        editor.apply();
    }


    /**
     * Sets the preference's value only if not already present
     * @param value the value to set
     */
    public void init(T value)
    {
        if (!isSet())
            set(value);
    }

    /**
     * Register a listener for this preference that gets notified each time it changes
     * @param callback the listener to register
     */
    public void registerListener(Consumer<Preference<T>> callback)
    {
        if (listeners.containsKey(callback))
            unregisterListener(callback);
        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, s) -> {
            if (s.equals(key))
                callback.accept(this);
        };
        listeners.put(callback, listener);
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Unregisters a previously added listener
     * @param callback the listener to remove
     */
    public void unregisterListener(Consumer<Preference<T>> callback)
    {
        SharedPreferences.OnSharedPreferenceChangeListener listener = listeners.remove(callback);
        if(listener != null)
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Remove all listeners
     */
    public void clearListeners()
    {
        for(Consumer<Preference<T>> key : Lists.newArrayList(listeners.keySet()))
            unregisterListener(key);
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        clearListeners();
    }

    @Override
    public String toString()
    {
        return "Preference{" + ", key='" + key + '\'' + ", value=" + String.format("%s", get()) + '}';
    }

    /**
     * Get this preference's value
     * @param preferences the shared preference to read from
     * @param key the preference's key
     * @param defaultValue default value to use if the preference is not present
     * @return this preference's value
     */
    protected abstract T getValue(SharedPreferences preferences, String key, @Nullable T defaultValue);

    /**
     * Sets this preference's value
     * @param editor the editor to use
     * @param key preference's key
     * @param value preference's value
     */
    protected abstract void setValue(SharedPreferences.Editor editor, String key, T value);
}
