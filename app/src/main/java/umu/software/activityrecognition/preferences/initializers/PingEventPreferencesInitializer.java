package umu.software.activityrecognition.preferences.initializers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;

import umu.software.activityrecognition.preferences.DialogflowServicePreferences;
import umu.software.activityrecognition.preferences.PingEventServicePreferences;

public class PingEventPreferencesInitializer implements Initializer<PingEventServicePreferences>
{
    @NonNull
    @Override
    public PingEventServicePreferences create(@NonNull Context context)
    {
        return new PingEventServicePreferences(context);
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }
}
