package umu.software.activityrecognition.shared;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;

public class RepeatingBroadcast extends BroadcastReceiver
{
    private BiConsumer<Context, Intent> mIntentConsumer;
    private final Context mContext;

    private PendingIntent mRepeatingIntent;
    private final String mAction = String.format("Action-%s", UUID.randomUUID());
    private final int mRequestCode = new Random().nextInt(10000) + 10000;
    private boolean mBroadcasting;

    public RepeatingBroadcast(@NonNull Context context)
    {
        mContext = context.getApplicationContext();
    }

    public String getBroadcastedAction()
    {
        return mAction;
    }


    public int getBroadcastedRequestCode()
    {
        return mRequestCode;
    }


    public boolean isBroadcasting()
    {
        return mBroadcasting;
    }


    public void start(long intervalMillis, BiConsumer<Context, Intent> intentConsumer)
    {
        if(isBroadcasting())
            stop();
        mBroadcasting = true;
        mIntentConsumer = intentConsumer;
        Intent intent = new Intent(mAction);
        buildRepeatingIntent(intent);
        IntentFilter intentFilter = createIntentFilter(intent);

        mRepeatingIntent = PendingIntent.getBroadcast(
                mContext,
                mRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        mContext.registerReceiver(this, intentFilter);
        AndroidUtils.getAlarmManager(mContext).setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                mRepeatingIntent
        );
    }

    public void stop()
    {
        if (!isBroadcasting()) return;
        mBroadcasting = false;
        AndroidUtils.getAlarmManager(mContext).cancel(mRepeatingIntent);
        mContext.unregisterReceiver(this);
    }


    protected void buildRepeatingIntent(@NonNull Intent intent) { }


    public IntentFilter createIntentFilter(@NonNull Intent intent)
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(intent.getAction());
        return filter;
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (mIntentConsumer != null)
            mIntentConsumer.accept(context, intent);
    }

}
