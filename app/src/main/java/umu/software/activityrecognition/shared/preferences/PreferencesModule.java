package umu.software.activityrecognition.shared.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.startup.AppInitializer;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import umu.software.activityrecognition.shared.preferences.Preferences;
import umu.software.activityrecognition.shared.preferences.PreferencesBuilder;

/**
 * A module providing access to a set of preferences. Subclasses can define named methods to access
 * the preferences being specifically accessed
 */
public abstract class PreferencesModule
{
    protected final Context mContext;
    private final SharedPreferences mPreferences;


    public PreferencesModule(Context context)
    {
        mContext = context.getApplicationContext();
        mPreferences = Preferences.DEFAULT.getInstance(context);
        initialize();
    }


    protected String getStringFromId(int resId)
    {
        return mContext.getString(resId);
    }

    /**
     * Get a preference and try to cast it to the given type
     * @param key preference key
     * @param defaultValue default value used if the preference if not found
     * @param castFunction the function using to cast the preference
     * @param <T> the type of the preference
     * @return
     */
    private <T> T getPreference(String key, T defaultValue, Function<String, T> castFunction)
    {
        Object value = mPreferences.getAll().get(key);
        if (value == null)
            return defaultValue;
        return castFunction.apply(value.toString());
    }


    /* Helper methods */

    protected String getString(String key, String defaultValue)
    {
        return getPreference(key, defaultValue, (s) -> s);
    }

    protected Boolean getBoolean(String key, boolean defaultValue)
    {
        return getPreference(key, defaultValue, Boolean::valueOf);
    }

    protected Float getFloat(String key, float defaultValue)
    {
        return getPreference(key, defaultValue, Float::valueOf);
    }

    protected Integer getInt(String key, int defaultValue)
    {
        return getPreference(key, defaultValue, Integer::valueOf);
    }

    protected Long getLong(String key, long defaultValue)
    {
        return getPreference(key, defaultValue, Long::valueOf);
    }

    protected String getString(int keyResId, String defaultValue)
    {
        return getPreference(getStringFromId(keyResId), defaultValue, (s) -> s);
    }

    protected Boolean getBoolean(int keyResId, boolean defaultValue)
    {
        return getPreference(getStringFromId(keyResId), defaultValue, Boolean::valueOf);
    }

    protected Float getFloat(int keyResId, float defaultValue)
    {
        return getPreference(getStringFromId(keyResId), defaultValue, Float::valueOf);
    }

    protected Integer getInt(int keyResId, int defaultValue)
    {
        return getPreference(getStringFromId(keyResId), defaultValue, Integer::valueOf);
    }

    protected Long getLong(int keyResId, long defaultValue)
    {
        return getPreference(getStringFromId(keyResId), defaultValue, Long::valueOf);
    }


    /**
     * Initialize the module
     */
    public void initialize()
    {
        PreferencesBuilder
                .newInstance((preferencesBuilder -> initialize(mContext, preferencesBuilder)))
                .accept(mPreferences);
    }

    /**
     * Initialize the properties of the module
     * @param context the calling context
     * @param builder helper builder
     */
    protected abstract void initialize(Context context, PreferencesBuilder builder);


}
