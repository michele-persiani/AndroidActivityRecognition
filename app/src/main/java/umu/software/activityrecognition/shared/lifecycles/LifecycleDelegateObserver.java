package umu.software.activityrecognition.shared.lifecycles;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class LifecycleDelegateObserver implements DefaultLifecycleObserver
{
    private final LifecycleRegistry mLifecycle;


    public LifecycleDelegateObserver(LifecycleRegistry delegatedLifecycle)
    {
        mLifecycle = delegatedLifecycle;
    }


    @Override
    public void onCreate(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    public void onStart(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void onResume(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onPause(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    }

    @Override
    public void onStop(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }

    @Override
    public void onDestroy(LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}
