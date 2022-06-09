package umu.software.activityrecognition.shared.lifecycles;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.BiConsumer;

import umu.software.activityrecognition.shared.RepeatingBroadcast;


public class RepeatingBroadcastLifecycle implements DefaultLifecycleObserver
{
    private final long mIntervalMillis;
    private final BiConsumer<Context, Intent> mIntentConsumer;
    private Context mContext;

    private RepeatingBroadcast mBroadcast;

    public RepeatingBroadcastLifecycle(@NonNull Context context, long intervalMillis, BiConsumer<Context, Intent> intentConsumer)
    {
        mContext = context.getApplicationContext();
        mIntervalMillis = Math.max(1000, intervalMillis);
        mIntentConsumer = intentConsumer;
    }


    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
        DefaultLifecycleObserver.super.onStart(owner);
        mBroadcast = new RepeatingBroadcast(mContext);
        mBroadcast.start(mIntervalMillis, mIntentConsumer);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
        DefaultLifecycleObserver.super.onStop(owner);
        mBroadcast.stop();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        DefaultLifecycleObserver.super.onDestroy(owner);
        mContext = null;
    }

}
