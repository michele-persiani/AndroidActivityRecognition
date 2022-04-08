package umu.software.activityrecognition.broadcastreceivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.VibrationEffect;
import android.util.Log;

import umu.software.activityrecognition.common.AndroidUtils;
import umu.software.activityrecognition.services.RecordService;

/**
 * BroadcastReceiver that calls that AlarmManager to invoke itself. At every invocation it will
 * use the saveZipClearFiles() procedure of the RecordService to save its current state
 */
public class RecurrentSave extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(RecurrentSave.class.getName(), "RecurrentSave onReceive()");

        RecordService.saveZipClearFiles(context);

        // Notify user through vibration
        AndroidUtils.getVibrator(context).vibrate(
                VibrationEffect.createOneShot(
                        500,
                        VibrationEffect.DEFAULT_AMPLITUDE)
        );
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
     * @param context calling context
     * @param intervalSeconds seconds in between each save
     */
    public static void startRecurrentSave(Context context, long intervalSeconds)
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
     *
     * @param context the calling context
     */
    public static void stopRecurrentSave(Context context)
    {
        AndroidUtils
                .getAlarmManager(context)
                .cancel(
                        getRepeatingIntent(context)
                );
    }
}