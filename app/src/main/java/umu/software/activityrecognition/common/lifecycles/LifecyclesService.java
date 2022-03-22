package umu.software.activityrecognition.common.lifecycles;

import android.app.Service;
import android.content.Intent;

public abstract class LifecyclesService extends Service
{
    private LifecycleComposite mLifecycles = new LifecycleComposite();


    protected void addLifecycleElement(LifecycleElement elem)
    {
        mLifecycles.addLifecycleElement(this, elem);
    }


    protected void removeLifecycleElement(LifecycleElement elem)
    {
        mLifecycles.removeLifecycleElement(this, elem);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mLifecycles.onCreate(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int result = super.onStartCommand(intent, flags, startId);
        mLifecycles.onStart(this);
        return result;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mLifecycles.onStop(this);
        mLifecycles.onDestroy(this);
    }
}
