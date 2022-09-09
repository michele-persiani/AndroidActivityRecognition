package umu.software.activityrecognition.shared.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Class that recurrently send broadcasts using the AlarmManager
 *
 */
public class RepeatingBroadcast extends BroadcastReceiver
{
    private BiConsumer<Context, Intent> mIntentConsumer;
    private final Context mContext;

    private PendingIntent mRepeatingIntent;
    private String mAction = String.format("umu.software.activityrecognition.%S-%s", getClass().getSimpleName(), UUID.randomUUID());
    private Bundle mExtras = new Bundle();
    private int mRequestCode = new Random().nextInt(10000) + 10000;
    private boolean mBroadcasting = false;


    public RepeatingBroadcast(@NonNull Context context)
    {
        mContext = context.getApplicationContext();
    }

    /**
     * Returns the action in the broadcasted intents
     * @return the action in the broadcasted intents
     */
    public String getBroadcastedAction()
    {
        return mAction;
    }

    /**
     * Sets the action contained in the broadcasted intent
     * @param action the action to set
     */
    public void setBroadcastedAction(String action)
    {
        mAction = action;
    }

    /**
     * Returns the request code in the broadcasted intents
     * @return the request code in the broadcasted intents
     */
    public int getBroadcastedRequestCode()
    {
        return mRequestCode;
    }

    /**
     * Sets the request code of the the broadcasted intents
     * @param requestCode request code to use
     */
    public void setBroadcastedRequestCode(int requestCode)
    {
        mRequestCode = requestCode;
    }


    public void setBroadcastedExtras(Bundle extras)
    {
        mExtras = extras;
    }


    /**
     *
     * @return whether its broadcasting
     */
    public boolean isBroadcasting()
    {
        return mBroadcasting;
    }

    /**
     * Start sending recurrent broadcasts. Stops the current broadcasting if already started
     * @param intervalMillis miliisecond of interval between a broadcast and the other
     * @param intentConsumer optional callback for the broadcasts
     */
    public void start(long intervalMillis, @Nullable BiConsumer<Context, Intent> intentConsumer)
    {
        if(isBroadcasting())
            stop();
        mBroadcasting = true;
        Intent intent = buildRepeatingIntent();
        IntentFilter intentFilter = createIntentFilter(intent);

        mRepeatingIntent = PendingIntent.getBroadcast(
                mContext,
                mRequestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        mIntentConsumer = intentConsumer;
        mContext.registerReceiver(this, intentFilter);
        AndroidUtils.getAlarmManager(mContext).setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                mRepeatingIntent
        );
    }

    /**
     * Stop broadcasting
     */
    public void stop()
    {
        if (!isBroadcasting()) return;
        mBroadcasting = false;
        AndroidUtils.getAlarmManager(mContext).cancel(mRepeatingIntent);
        mContext.unregisterReceiver(this);
    }

    /**
     * Builds the intent that will be broadcasted.
     */
    protected Intent buildRepeatingIntent()
    {
        Intent intent = new Intent();
        intent.setAction(mAction);
        intent.putExtras(new Bundle());
        intent.getExtras().putAll(mExtras);
        return intent;
    }

    /**
     *
     * @param intent the intent obtained through buildRepeatingIntent()
     * @return the intent filter to receive the broadcasted intents
     */
    protected IntentFilter createIntentFilter(@NonNull Intent intent)
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
