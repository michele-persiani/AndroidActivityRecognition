package umu.software.activityrecognition.shared.lifecycles;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;


/**
 * Forwards lifecycle calls to a wrapped LifecycleRegistry
 */
public class LifecycleDelegateObserver implements DefaultLifecycleObserver
{
    private final LifecycleRegistry mLifecycle;


    public LifecycleDelegateObserver(LifecycleRegistry delegatedLifecycle)
    {
        mLifecycle = delegatedLifecycle;
    }


    @Override
    public void onCreate(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}
