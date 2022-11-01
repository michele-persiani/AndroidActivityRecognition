package umu.software.activityrecognition.speech;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.startup.Initializer;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpeechIOInitializer implements Initializer<Object>
{
    @NonNull
    @Override
    public Object create(@NonNull @NotNull Context context)
    {
        ASR.getInstance().initialize(context);
        TTS.getInstance().initialize(context);
        return new Object();
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies()
    {
        return Lists.newArrayList();
    }
}
