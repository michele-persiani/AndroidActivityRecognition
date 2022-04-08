package umu.software.activityrecognition.common;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to access various android components
 */
public class AndroidUtils
{
    private static int numHandlers = 0;

    /* Handlers */

    public static Handler newHandler()
    {
        numHandlers += 1;
        HandlerThread handlerThread = new HandlerThread("Handler-"+numHandlers);
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    public static Handler newMainLooperHandler()
    {
        return new Handler(Looper.getMainLooper());
    }









    /* System services */

    public static Vibrator getVibrator(Context context)
    {
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static SensorManager getSensorManager(Context context)
    {
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }


    public static NotificationManager getNotificationManager(Context context)
    {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    public static AlarmManager getAlarmManager(Context context)
    {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }


    public static PowerManager getPowerManager(Context context)
    {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);

    }


    public static PowerManager.WakeLock getWakeLock(Context context, int wakeLockType)
    {
        PowerManager powerManager = getPowerManager(context);
        return powerManager.newWakeLock(
                wakeLockType,
                context.getClass().getSimpleName() +"/"+ wakeLockType);
    }




    /* Files utils */

    /**
     * Moves a file from the Assets folder to the temporary cache directory.
     * @param context Andoid context, such as an activity
     * @param fileName name of the
     * @return the file in the cache directory
     * @throws IOException
     */
    public static File moveAssetToCache(Context context, String fileName) throws IOException
    {
        File f = new File(String.format("%s%s%s", context.getCacheDir(), File.separator, fileName));

        if (!f.exists())
        {
            InputStream is = context.getAssets().open(fileName);
            byte[] buffer = new byte[2048];
            is.read(buffer);
            is.close();


            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        }
        return f;
    }

}
