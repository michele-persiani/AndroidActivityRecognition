package umu.software.activityrecognition.application;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Used to capture the first onStart() and onResume() called by the first launched activity
 */
class ApplicationActivityLifecycle implements Application.ActivityLifecycleCallbacks
{
    private final LifecycleRegistry mRegistry;
    private boolean mOnStartTriggered = false;
    private boolean mOnResumeTriggered = false;


    ApplicationActivityLifecycle(LifecycleRegistry registry)
    {
        mRegistry = registry;
    }


    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle)
    {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity)
    {
        if (mOnStartTriggered) return;
        mOnStartTriggered = true;
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity)
    {
        if (mOnResumeTriggered) return;
        mOnResumeTriggered = true;
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle)
    {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity)
    {

    }
}
