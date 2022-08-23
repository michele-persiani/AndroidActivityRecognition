package umu.software.activityrecognition.shared.lifecycles;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import umu.software.activityrecognition.shared.util.AndroidUtils;


/**
 * Wrapper for the device wake lock
 */
public class WakeLockLifecycle implements DefaultLifecycleObserver
{
    private final int wakeLockType;
    private final Context context;
    private PowerManager.WakeLock mWakeLock;

    public WakeLockLifecycle(Context context, int wakeLockType)
    {
        this.context = context.getApplicationContext();
        this.wakeLockType = wakeLockType;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
        if (mWakeLock != null)
            return;
        mWakeLock = AndroidUtils.getWakeLock(context, wakeLockType);
        mWakeLock.acquire();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();
    }


    public static LifecycleObserver newWakeLock(Context context, int level)
    {
        return new WakeLockLifecycle(context, level);
    }

    public static LifecycleObserver newPartialWakeLock(Context context)
    {
        return new WakeLockLifecycle(context, PowerManager.PARTIAL_WAKE_LOCK);
    }
}
