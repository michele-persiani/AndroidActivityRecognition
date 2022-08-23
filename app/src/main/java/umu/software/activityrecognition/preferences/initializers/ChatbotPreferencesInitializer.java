package umu.software.activityrecognition.preferences.initializers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;

import umu.software.activityrecognition.preferences.DialogflowServicePreferences;

public class ChatbotPreferencesInitializer implements Initializer<DialogflowServicePreferences>
{
    @NonNull
    @Override
    public DialogflowServicePreferences create(@NonNull Context context)
    {
        return new DialogflowServicePreferences(context);
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }
}
