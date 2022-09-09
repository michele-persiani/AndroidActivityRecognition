package umu.software.activityrecognition.application;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import umu.software.activityrecognition.services.recordings.RecordServiceHelper;
import umu.software.activityrecognition.shared.preferences.Preferences;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.TTS;
import umu.software.activityrecognition.speech.translate.LanguageTranslation;

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

        //getPreferences().getInstance(this).edit().clear().apply();
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
        sContext = null;
    }


    @NonNull
    public static ApplicationSingleton getInstance()
    {
        return sContext;
    }


    @NonNull
    public static Context getContext()
    {
        return sContext;
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
