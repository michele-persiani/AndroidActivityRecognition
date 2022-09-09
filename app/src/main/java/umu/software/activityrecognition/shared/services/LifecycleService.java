package umu.software.activityrecognition.shared.services;

import android.app.Service;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import umu.software.activityrecognition.shared.util.LogHelper;

/**
 * Started service with an associated lifecycle
 * onCreate() will invoke start event
 * onStartCommand() the first call will invoke start, resume events. Subsequent calls won't throw lifecycle events
 * onDestroy() will invoke pause, stop, destroy events
 */
public abstract class LifecycleService extends Service implements LifecycleOwner, LifecycleEventObserver
{
    public static final String ACTION_STATE_CHANGED = "umu.software.activityrecognition.ACTION_STATE_CHANGED";
    public static final String EXTRA_CURRENT_STATE  = "EXTRA_CURRENT_STATE";
    public static final String EXTRA_EVENT          = "EXTRA_EVENT";
    public static final String EXTRA_SERVICE        = "EXTRA_SERVICE";

    private LogHelper mLog;
    private LifecycleRegistry mLifecycle;


    @NonNull
    public LifecycleRegistry getLifecycle()
    {
        return mLifecycle;
    }



    @Override
    public void onCreate()
    {
        super.onCreate();
        mLifecycle = new LifecycleRegistry(this);
        mLog = LogHelper.newClassTag(this);
        mLifecycle.addObserver(this);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        logger().i("onStartCommand() -> %s", intent);
        int result = super.onStartCommand(intent, flags, startId);
        if (mLifecycle.getCurrentState().equals(Lifecycle.State.CREATED))
        {
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }
        return result;
    }


    @Override
    public void onDestroy()
    {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }


    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event)
    {
        Intent intent = new Intent();
        intent.setAction(ACTION_STATE_CHANGED);
        intent.putExtra(EXTRA_CURRENT_STATE, source.getLifecycle().getCurrentState());
        intent.putExtra(EXTRA_EVENT, event);
        intent.putExtra(EXTRA_SERVICE, getClass());
        sendBroadcast(intent);
    }

    protected LogHelper logger()
    {
        return mLog;
    }

    /**
     * Checks whether the given event describes a lifecycle event for the given service and event.
     * ie. the intent's action is ACTION_STATE_CHANGED, and its extras EXTRA_CURRENT_STATE, EXTRA_EVENT, EXTRA_SERVICE
     * matches with the provided function parameters
     * @param intent intent to test
     * @param serviceClass tested class of the LifecycleStartedService
     * @param event tested lifecycle event
     * @param <S> subclass of LifecycleStartedService
     * @return true or false
     */
    public static <S extends LifecycleService> boolean isLifecycleBroadcast(Intent intent, Class<S> serviceClass, Lifecycle.Event event)
    {
        if (!intent.getAction().equals(ACTION_STATE_CHANGED) || !intent.hasExtra(EXTRA_SERVICE) || !intent.hasExtra(EXTRA_EVENT))
            return false;
        return intent.getSerializableExtra(EXTRA_EVENT) == event &&
                intent.getSerializableExtra(EXTRA_SERVICE).equals(serviceClass);
    }
}
