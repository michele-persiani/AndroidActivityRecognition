package umu.software.activityrecognition.config;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.google.common.collect.Lists;

import java.util.List;


public class PreferencesInitializers
{
    public static class RecordingsPreferencesInitializer implements Initializer<RecordingsPreferences>
    {
        @NonNull
        @Override
        public RecordingsPreferences create(@NonNull Context context)
        {
            return new RecordingsPreferences(context);
        }

        @NonNull
        @Override
        public List<Class<? extends Initializer<?>>> dependencies()
        {
            return Lists.newArrayList();
        }
    }


    public static class ChatbotPreferencesInitializer implements Initializer<ChatBotPreferences>
    {
        @NonNull
        @Override
        public ChatBotPreferences create(@NonNull Context context)
        {
            return new ChatBotPreferences(context);
        }

        @NonNull
        @Override
        public List<Class<? extends Initializer<?>>> dependencies()
        {
            return Lists.newArrayList();
        }
    }
}
