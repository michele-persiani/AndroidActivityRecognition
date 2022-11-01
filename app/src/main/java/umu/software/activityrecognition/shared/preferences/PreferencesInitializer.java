package umu.software.activityrecognition.shared.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * Initializer for PreferencesModule objects. Modules should register for initialization by
 * static {
 *     PreferencesInitializer.addInitialization(ModuleClassName.class)
 * }
 */
public class PreferencesInitializer implements Initializer<PreferencesModule>
{
    private static final ArrayList<Class<? extends PreferencesModule>> preferences = Lists.newArrayList();



    /**
     * Adds a PreferenceModule class to be initialized ad startup
     * @param pref class of the preferences module to initialize at startup
     */
    public static void addInitialization(Class<? extends PreferencesModule> pref)
    {
        preferences.add(pref);
    }


    @NonNull
    @Override
    public PreferencesModule create(@NonNull Context context)
    {
        for (Class<? extends PreferencesModule> cls : preferences)
            Exceptions.runCatch(() -> cls.getConstructor(Context.class).newInstance(context).initialize());

        return new PreferencesModule(context)
        {
            @Override
            protected void initialize()
            {

            }
        };
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }
}
