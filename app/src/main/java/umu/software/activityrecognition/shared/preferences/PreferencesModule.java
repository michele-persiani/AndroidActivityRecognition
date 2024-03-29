package umu.software.activityrecognition.shared.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A module providing access to a set of preferences. Subclasses can define named methods to access
 * the preferences being specifically accessed.
 * NB. when a module is finalized all preference observers are detached from its preferences
 */
public abstract class PreferencesModule
{
    protected final Context mContext;
    protected final SharedPreferences mSharedPreferences;

    private final Map<String, Preference<?>> mPreferences = Maps.newHashMap();


    public PreferencesModule(Context context)
    {
        mContext = context.getApplicationContext();
        mSharedPreferences = NamedSharedPreferences.DEFAULT_PREFERENCES.getInstance(mContext);
        initialize();
    }


    protected String getStringRes(int resId)
    {
        return mContext.getString(resId);
    }


    protected Resources getResources()
    {
        return mContext.getResources();
    }

    /**
     * Gets a flyweight preference that accesses a specific preference of SharedPreferences
     * @param key the preference's key
     * @param factoryMethod function that uses PreferenceFactory to create a Preference<T></T>
     * @param <T> datatype of the preference
     * @return a cached Preference if present (with the given key) or a newly created Preference that is added to the cache
     */
    public <T> Preference<T> getPreference(String key, Function<PreferenceFactory, Preference<T>> factoryMethod)
    {
        if (!mPreferences.containsKey(key))
        {
            PreferenceFactory factory = PreferenceFactory.newInstance(mSharedPreferences);
            Preference<T> pref = factoryMethod.apply(factory);
            mPreferences.put(key, pref);
        }
        return (Preference<T>) mPreferences.get(key);
    }


    /* Helper methods to get Preferences for standard datatypes */

    public Preference<String> getString(String key)
    {
        return getPreference(key, f -> f.stringPreference(key));
    }

    public Preference<String> getString(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getString(key);
    }

    public Preference<Boolean> getBoolean(String key)
    {
        return getPreference(key, f -> f.booleanPreference(key));
    }

    public Preference<Boolean> getBoolean(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getBoolean(key);
    }

    public Preference<Float> getFloat(String key)
    {
        return getPreference(key, f -> f.floatPreference(key));
    }

    public Preference<Float> getFloat(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getFloat(key);
    }

    public Preference<Integer> getInt(String key)
    {
        return getPreference(key, f -> f.integerPreference(key));
    }

    public Preference<Integer> getInt(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getInt(key);
    }

    public Preference<Long> getLong(String key)
    {
        return getPreference(key, f -> f.longPreference(key));
    }

    public Preference<Long> getLong(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getLong(key);
    }

    public Preference<Set<String>> getStringSet(String key)
    {
        return getPreference(key, f -> f.stringSetPreference(key));
    }


    public Preference<Set<String>> getStringSet(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getStringSet(key);
    }


    public Preference<List<String>> getStringList(String key)
    {
        return getPreference(key, f -> f.stringListPreference(key));
    }


    public Preference<List<String>> getStringList(int keyResId)
    {
        String key = getStringRes(keyResId);
        return getStringList(key);
    }


    /**
     * Clear all listeners of all Preference of this module
     */
    public void clearListeners()
    {
        for (Preference<?> p : mPreferences.values())
            p.clearListeners();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        clearListeners();
        mPreferences.clear();
    }


    /**
     * Initialize the properties of the module. Use with Preferences' init() metohds
     */
    protected abstract void initialize();


}
