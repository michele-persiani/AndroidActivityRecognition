package umu.software.activityrecognition.shared.preferences;

import android.content.SharedPreferences;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;


/**
 * Factory to create Preference<T> objects
 */
public class PreferenceFactory
{
    private final SharedPreferences preferences;

    private PreferenceFactory(SharedPreferences preferences)
    {
        this.preferences = preferences;
    }

    public static PreferenceFactory newInstance(SharedPreferences preferences)
    {
        return new PreferenceFactory(preferences);
    }

    public Preference<String> stringPreference(String key)
    {
        return new Preference<String>(preferences, key)
        {
            @Override
            protected String getValue(SharedPreferences preferences, String key, String defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return prefMap.get(key).toString();
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, String value)
            {
                editor.putString(key, value);
            }
        };
    }

    public Preference<Boolean> booleanPreference(String key)
    {
        return new Preference<Boolean>(preferences, key)
        {
            @Override
            protected Boolean getValue(SharedPreferences preferences, String key, Boolean defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Boolean.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Boolean value)
            {
                editor.putBoolean(key, value);
            }
        };
    }

    public Preference<Integer> integerPreference(String key)
    {
        return new Preference<Integer>(preferences, key)
        {
            @Override
            protected Integer getValue(SharedPreferences preferences, String key, Integer defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Integer.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Integer value)
            {
                editor.putInt(key, value);
            }
        };
    }

    public Preference<Float> floatPreference(String key)
    {
        return new Preference<Float>(preferences, key)
        {
            @Override
            protected Float getValue(SharedPreferences preferences, String key, Float defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Float.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Float value)
            {
                editor.putFloat(key, value);
            }
        };
    }

    public Preference<Long> longPreference(String key)
    {
        return new Preference<Long>(preferences, key)
        {
            @Override
            protected Long getValue(SharedPreferences preferences, String key, Long defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Long.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Long value)
            {
                editor.putLong(key, value);
            }
        };
    }


    public Preference<Long> stringPersistedLongPreference(String key)
    {
        return new Preference<Long>(preferences, key)
        {
            @Override
            protected Long getValue(SharedPreferences preferences, String key, Long defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Long.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Long value)
            {
                editor.putString(key, value.toString());
            }
        };
    }



    public Preference<Integer> stringPersistedIntegerPreference(String key)
    {
        return new Preference<Integer>(preferences, key)
        {
            @Override
            protected Integer getValue(SharedPreferences preferences, String key, Integer defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Integer.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Integer value)
            {
                editor.putString(key, value.toString());
            }
        };
    }

    public Preference<Float> stringPersistedFloatPreference(String key)
    {
        return new Preference<Float>(preferences, key)
        {
            @Override
            protected Float getValue(SharedPreferences preferences, String key, Float defaultValue)
            {
                Map<String, ?> prefMap = preferences.getAll();
                if (!prefMap.containsKey(key)) return defaultValue;
                return Float.valueOf(prefMap.get(key).toString());
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Float value)
            {
                editor.putString(key, value.toString());
            }
        };
    }

    public Preference<Set<String>> stringSetPreference(String key)
    {
        return new Preference<Set<String>>(preferences, key)
        {
            @Override
            protected Set<String> getValue(SharedPreferences preferences, String key, Set<String> defaultValue)
            {
                Set<String> set = preferences.getStringSet(key, defaultValue);
                return Sets.newTreeSet(set);
            }

            @Override
            protected void setValue(SharedPreferences.Editor editor, String key, Set<String> value)
            {
                editor.putStringSet(key, value);
            }
        };
    }
}
