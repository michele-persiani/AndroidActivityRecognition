package umu.software.activityrecognition.shared.preferences;

import android.content.SharedPreferences;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.function.Consumer;


/**
 * Helper class to edit SharedPreferences
 */
public abstract class PreferencesEditor implements Consumer<SharedPreferences>
{
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    public void accept(SharedPreferences preferences)
    {
        prefs = preferences;
        editor = preferences.edit();
        performEdit();
        editor.commit();
        prefs = null;
        editor = null;
    }

    private void initValue(String k, Consumer<SharedPreferences.Editor> builder)
    {
        if (!prefs.contains(k))
            builder.accept(editor);
    }

    private void putValue(Consumer<SharedPreferences.Editor> builder)
    {
        builder.accept(editor);
    }

    protected abstract void performEdit();

    /*
    Functions to initialize values. Values are initialized only if not already present in the preferences
     */

    public void initInt(String k, int v) { initValue(k, (e) -> e.putInt(k, v));}

    public void initFloat(String k, float v) { initValue(k, (e) -> e.putFloat(k, v));}

    public void initLong(String k, long v) { initValue(k, (e) -> e.putLong(k, v));}

    public void initString(String k, String v) { initValue(k, (e) -> e.putString(k, v));}

    public void initBoolean(String k, boolean v) { initValue(k, (e) -> e.putBoolean(k, v));}

    public void initStringSet(String k, Set<String> v) { initValue(k, (e) -> e.putStringSet(k, v));}

    public void initStringSet(String k, String... v) { initValue(k, (e) -> e.putStringSet(k, Sets.newHashSet(v)));}

    /*
    Functions to modify values.
     */

    public void putInt(String k, int v) { putValue((e) -> e.putInt(k, v));}

    public void putFloat(String k, float v) { putValue((e) -> e.putFloat(k, v));}

    public void putLong(String k, long v) { putValue((e) -> e.putLong(k, v));}

    public void putString(String k, String v) { putValue((e) -> e.putString(k, v));}

    public void putBoolean(String k, boolean v) { putValue((e) -> e.putBoolean(k, v));}

    public void putStringSet(String k, Set<String> v) { putValue((e) -> e.putStringSet(k, v));}

    public void putStringSet(String k, String... v) { putValue((e) -> e.putStringSet(k, Sets.newHashSet(v)));}



    public static PreferencesEditor newInstance(Consumer<PreferencesEditor> builder)
    {
        return new PreferencesEditor()
        {
            @Override
            protected void performEdit()
            {
                builder.accept(this);
            }
        };
    }
}
