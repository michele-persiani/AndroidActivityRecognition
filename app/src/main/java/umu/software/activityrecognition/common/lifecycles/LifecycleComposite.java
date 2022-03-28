package umu.software.activityrecognition.common.lifecycles;

import android.content.Context;

import java.util.HashSet;
import java.util.function.Consumer;

public class LifecycleComposite implements LifecycleElement
{
    private HashSet<LifecycleElement> mElements = new HashSet<>();

    private LifecycleElement.LifecycleState mState = LifecycleElement.LifecycleState.DESTROYED;


    public void addLifecycleElement(Context context, LifecycleElement element)
    {
        if (mElements.contains(element))
            return;
        switch (mState)
        {
            case DESTROYED:
            case CREATED:
                element.onCreate(context);
            case STARTED:
                element.onCreate(context);
                element.onStart(context);
            case STOPPED:
                element.onCreate(context);
            default:
                mElements.add(element);
        }
    }


    public void removeLifecycleElement(Context context, LifecycleElement element)
    {
        if (!mElements.contains(element))
            return;
        switch (mState)
        {
            case DESTROYED:
            case CREATED:
                element.onDestroy(context);
            case STARTED:
                element.onStop(context);
                element.onDestroy(context);
            case STOPPED:
                element.onDestroy(context);
            default:
                mElements.remove(element);
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
