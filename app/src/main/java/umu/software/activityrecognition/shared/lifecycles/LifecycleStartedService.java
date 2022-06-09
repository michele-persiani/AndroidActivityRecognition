package umu.software.activityrecognition.shared.lifecycles;

import android.app.Service;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.android.gms.dynamic.LifecycleDelegate;


public abstract class LifecycleStartedService extends Service implements LifecycleOwner
{
    private LifecycleRegistry mLifecycle;


    public Lifecycle getLifecycle()
    {
        return mLifecycle;
    }



    @Override
    public void onCreate()
    {
        super.onCreate();
        mLifecycle = new LifecycleRegistry(this);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int result = super.onStartCommand(intent, flags, startId);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        return result;
    }

    public void resumeLifecycle()
    {
        if (mLifecycle.getCurrentState().equals(Lifecycle.State.STARTED))
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    public void pauseLifecycle()
    {
        if (mLifecycle.getCurrentState().equals(Lifecycle.State.RESUMED))
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        pauseLifecycle();
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

}
