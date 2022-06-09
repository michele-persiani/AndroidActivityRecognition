package umu.software.activityrecognition.application;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import umu.software.activityrecognition.data.sensors.Sensors;
import umu.software.activityrecognition.services.RecordServiceHelper;
import umu.software.activityrecognition.shared.preferences.Preferences;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.TTS;

public class ApplicationSingleton extends Application implements LifecycleOwner
{
    private static ApplicationSingleton sContext;

    private LifecycleRegistry mLifecycle;


    @Override
    public void onCreate()
    {
        super.onCreate();
        sContext = this;
        mLifecycle = new LifecycleRegistry(this);

        ASR.FREE_FORM.initialize(this);
        TTS.INSTANCE.initialize(this);
        Sensors.INSTANCE.initialize(this);

        registerActivityLifecycleCallbacks(new ApplicationActivityLifecycle(mLifecycle));
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }



    @Override
    public void onTerminate()
    {
        super.onTerminate();
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        Sensors.INSTANCE.destroy();
        sContext = null;
    }


    @NonNull
    public ApplicationSingleton getInstance()
    {
        return sContext;
    }


    @NonNull
    public static Context getContext()
    {
        return sContext;
    }



    public ASR getSpeechRecognizer()
    {
        return ASR.FREE_FORM;
    }


    @NonNull
    public TTS getTextToSpeech()
    {
        return TTS.INSTANCE;
    }


    @NonNull
    public Sensors getSensors()
    {
        return Sensors.INSTANCE;
    }

    @NonNull
    public Preferences getPreferences()
    {
        return Preferences.DEFAULT;
    }


    @NonNull
    public RecordServiceHelper getRecordServiceHelper()
    {
        return RecordServiceHelper.newInstance(this);
    }

    @NonNull
    public Lifecycle getLifecycle()
    {
        return mLifecycle;
    }
}
