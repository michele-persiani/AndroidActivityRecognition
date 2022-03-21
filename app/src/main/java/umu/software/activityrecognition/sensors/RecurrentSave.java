package umu.software.activityrecognition.sensors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import umu.software.activityrecognition.common.AndroidUtils;

/**
 * BroadcastReceiver that calls that AlarmManager to invoke itself. At every invocation it will
 * use the saveZipClearFiles() procedure of the RecordService to save its current state
 */
public class RecurrentSave extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(RecordService.class.getName(), "RecurrentSave onReceive()");
        RecordService.saveZipClearFiles(context);
    }

    public static PendingIntent getRepeatingIntent(Context context)
    {
        Intent intent = new Intent(context, RecurrentSave.class);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Start to recurrently save
     * @param context
     * @param intervalSeconds
     */
    public static void start(Context context, long intervalSeconds)
    {

        AndroidUtils
                .getAlarmManager(context)
                .setInexactRepeating(
                        AlarmManager.RTC,
                        System.currentTimeMillis(),
                        intervalSeconds * 1000,
                        getRepeatingIntent(context)
                );
    }

    /**
     * Start to recurrently save every 10 minutes
     * @param context
     */
    public static void start(Context context)
    {
        AndroidUtils
                .getAlarmManager(context)
                .setInexactRepeating(
                        AlarmManager.RTC,
                        System.currentTimeMillis(),
                        600 * 1000, // 10 minutes
                        getRepeatingIntent(context)
                );
    }

    /**
     *
     * @param context
     */
    public static void stop(Context context)
    {
        AndroidUtils
                .getAlarmManager(context)
                .cancel(
                        getRepeatingIntent(context)
                );
    }
}