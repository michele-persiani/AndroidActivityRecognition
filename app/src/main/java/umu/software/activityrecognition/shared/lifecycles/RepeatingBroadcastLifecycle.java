package umu.software.activityrecognition.shared.lifecycles;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.WorkManager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import umu.software.activityrecognition.shared.util.RepeatingBroadcast;


/**
 * Lifecycle handling a RepeatingBroadcast
 */
public class RepeatingBroadcastLifecycle implements DefaultLifecycleObserver
{
    private long mIntervalMillis;
    private final BiConsumer<Context, Intent> mIntentConsumer;
    private Context mContext;

    private RepeatingBroadcast mBroadcast;


    public RepeatingBroadcastLifecycle(@NonNull Context context, long intervalMillis, @Nullable BiConsumer<Context, Intent> intentConsumer)
    {
        mContext = context.getApplicationContext();
        mIntervalMillis = Math.max(1000, intervalMillis);
        mIntentConsumer = intentConsumer;
    }

    /**
     * Configure the broadcast receiver through a builder method
     * @param broadcastBuilder builder method
     */
    public void configure(Consumer<RepeatingBroadcast> broadcastBuilder)
    {
        broadcastBuilder.accept(mBroadcast);
    }

    public boolean isBroadcasting()
    {
        return mBroadcast != null && mBroadcast.isBroadcasting();
    }


    public void setIntervalMillis(long intervalMillis)
    {
        mIntervalMillis = intervalMillis;
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
        DefaultLifecycleObserver.super.onDestroy(owner);
        mBroadcast.stop();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        DefaultLifecycleObserver.super.onDestroy(owner);
        mContext = null;
    }

}
