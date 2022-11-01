package umu.software.activityrecognition.shared.lifecycles;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;


/**
 * Forwards lifecycle calls to a wrapped LifecycleRegistry
 */
public class LifecycleDelegateObserver implements LifecycleEventObserver
{
    private final LifecycleRegistry mLifecycle;


    public LifecycleDelegateObserver(LifecycleRegistry delegatedLifecycle)
    {
        mLifecycle = delegatedLifecycle;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event)
    {
        mLifecycle.handleLifecycleEvent(event);
    }

}
