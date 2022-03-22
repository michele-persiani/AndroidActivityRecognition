package umu.software.activityrecognition.common.lifecycles;

import android.content.Context;

import java.util.HashSet;
import java.util.function.Consumer;

public class LifecycleComposite implements LifecycleElement
{
    private HashSet<LifecycleElement> mElements = new HashSet<>();

    private LifecycleElement.LifecycleState mState = LifecycleElement.LifecycleState.DESTROYED;


    public void addLifecycleElement(Context context, LifecycleElement component)
    {
        switch (mState)
        {
            case DESTROYED:
            case CREATED:
                component.onCreate(context);
            case STARTED:
                component.onCreate(context);
                component.onStart(context);
            case STOPPED:
                component.onCreate(context);
            default:
                mElements.add(component);
        }
    }


    public void removeLifecycleElement(Context context, LifecycleElement component)
    {
        if (!mElements.contains(component))
            return;
        switch (mState)
        {
            case DESTROYED:
            case CREATED:
                component.onDestroy(context);
            case STARTED:
                component.onStop(context);
                component.onDestroy(context);
            case STOPPED:
                component.onDestroy(context);
            default:
                mElements.remove(component);
        }
    }


    private void forEachComponents(Consumer<LifecycleElement> component)
    {
        for (LifecycleElement c : mElements)
            component.accept(c);
    }

    @Override
    public void onCreate(Context context)
    {
        forEachComponents(c -> c.onCreate(context));
    }

    @Override
    public void onStart(Context context)
    {
        forEachComponents(c -> c.onStart(context));
    }

    @Override
    public void onStop(Context context)
    {
        forEachComponents(c -> c.onStop(context));
    }

    @Override
    public void onDestroy(Context context)
    {
        forEachComponents(c -> c.onDestroy(context));
    }
}
