package umu.software.activityrecognition.preferences.initializers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;

import umu.software.activityrecognition.preferences.RecordServicePreferences;

public class RecordingsPreferencesInitializer implements Initializer<RecordServicePreferences>
{
    @NonNull
    @Override
    public RecordServicePreferences create(@NonNull Context context)
    {
        return new RecordServicePreferences(context);
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }
}