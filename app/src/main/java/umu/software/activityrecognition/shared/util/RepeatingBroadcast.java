package umu.software.activityrecognition.shared.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Class that recurrently send broadcasts using the AlarmManager
 *
 */
public class RepeatingBroadcast extends BroadcastReceiver
{

    private class BroadcastThread extends Thread
    {

        private final long mSleepMillis;

        BroadcastThread(long sleepMillis)
        {
            mSleepMillis = sleepMillis;
        }

        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    Thread.sleep(mSleepMillis);
                } catch (InterruptedException e)
                {
                    return;
                }

                sendBrodcast();
            }
        }
    }


    private BiConsumer<Context, Intent> mIntentConsumer;
    private final Context mContext;

    private Intent mRepeatingIntent;
    private final String mAction = String.format("umu.software.activityrecognition.%S-%s", getClass().getSimpleName(), UUID.randomUUID());
    private boolean mBroadcasting = false;

    private Consumer<Intent> mIntentBuilder;
    private BroadcastThread mThread;

    public RepeatingBroadcast(@NonNull Context context)
    {
        mContext = context.getApplicationContext();
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (mIntentConsumer != null)
            mIntentConsumer.accept(context, intent);
    }

    /**
     * Sets a builder to construct the braodcasted intents
     * @param builder intent builder
     */
    public void setIntentBuilder(Consumer<Intent> builder)
    {
        mIntentBuilder = builder;
    }

    /**
     * Returns whether the recurrent broadcasting is active
     * @return whether its broadcasting
     */
    public boolean isBroadcasting()
    {
        return mBroadcasting;
    }


    /**
     * Start sending recurrent broadcasts, stopping the current broadcasting if already started.
     * The first broadcast is sent after 'intervalMillis' have passed.
     * Use sendBroadcast() right after start() to send an immediate broadcast
     * @param intervalMillis millisecond of interval between a broadcast and the other
     * @param intentConsumer optional callback for the broadcasts
     */
    public synchronized void start(long intervalMillis, @Nullable BiConsumer<Context, Intent> intentConsumer)
    {
        if(isBroadcasting())
            stop();
        mBroadcasting = true;

        mRepeatingIntent = new Intent();
        mRepeatingIntent.setAction(mAction);
        if (mIntentBuilder != null)
            mIntentBuilder.accept(mRepeatingIntent);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(mRepeatingIntent.getAction());


        mIntentConsumer = intentConsumer;
        LocalBroadcastManager.getInstance(mContext).registerReceiver(this, intentFilter);

        // Start thread
        intervalMillis = Math.max(1, intervalMillis);
        mThread = new BroadcastThread(intervalMillis);
        mThread.start();
    }

    /**
     * Stop broadcasting
     */
    public synchronized void stop()
    {
        if (!isBroadcasting()) return;
        mBroadcasting = false;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(this);
        // Stop thread
        mThread.interrupt();
    }

    /**
     * Broadcasts the intent. Active only while isBroadcasting is true
     */
    public synchronized void sendBrodcast()
    {
        if (isBroadcasting())
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(mRepeatingIntent);
    }

}
