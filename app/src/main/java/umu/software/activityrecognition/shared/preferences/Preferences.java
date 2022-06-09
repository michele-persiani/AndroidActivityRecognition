package umu.software.activityrecognition.shared.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.function.Consumer;
import java.util.function.Function;

import umu.software.activityrecognition.shared.AndroidUtils;

/**
 * Singleton class to access SharedPreferences
 */
public enum Preferences
{
    DEFAULT(AndroidUtils::getDefaultSharedPreferences);


    private final Function<Context, SharedPreferences> prefFunction;

    Preferences(Function<Context, SharedPreferences> prefFunction)
    {
        this.prefFunction = prefFunction;
    }

    
    public SharedPreferences getInstance(Context context)
    {
        return prefFunction.apply(context);
    }


    public void applyBuilder(Context context, Consumer<PreferencesBuilder> builder)
    {
        SharedPreferences pref = getInstance(context);
        PreferencesBuilder.newInstance(builder).accept(pref);
    }

}
