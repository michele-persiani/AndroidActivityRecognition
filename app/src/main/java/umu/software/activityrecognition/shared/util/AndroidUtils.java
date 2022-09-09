package umu.software.activityrecognition.shared.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utility class to access various android components
 */
public class AndroidUtils
{
    private static int numHandlers = 0;

    private AndroidUtils() {}





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



    /* Power utils */

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

    public static SharedPreferences getDefaultSharedPreferences(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }


    /* Connectivity utils */

    public static ConnectivityManager getConnectivityManager(Context context)
    {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static WifiManager getWifiManager(Context context)
    {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }


    /**
     * Switch on the wifi and forces it to be on. Requires permission CHANGE_WIFI_STATE
     * @param context the calling context
     * @return the WifiLock locking the wifi to on. The lock is already acquired and should be released after usage.
     */
    public static WifiManager.WifiLock forceWifiOn(Context context)
    {
        WifiManager wifiManager = getWifiManager(context);
        WifiManager.WifiLock lock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                String.format("%s", UUID.randomUUID())
        );
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED)
            wifiManager.setWifiEnabled(true);
        lock.acquire();
        return lock;
    }





    /* Files utils */

    /**
     * Moves a file from the Assets folder to the temporary cache directory.
     * @param context Andoid context, such as an activity
     * @param fileName name of the
     * @return the file in the cache directory
     * @throws IOException if something bad happens
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

    /**
     * Read the content of a resource in the 'res/raw' folder.  Uses UTF-8 charset
     * @param context the calling context
     * @param rawResourceId R.raw.* resource id
     * @return the file content as a string, or null if something was bad
     */
    @Nullable
    public static String readRawResourceFile(Context context, int rawResourceId)
    {
        InputStream inputStream = context.getResources().openRawResource(rawResourceId);
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();

        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0)
                result.append(buffer, 0, charsRead);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    /* Various */

    public static long measureElapsedTime(Runnable run)
    {
        long time = SystemClock.elapsedRealtime();
        run.run();
        time = SystemClock.elapsedRealtime() - time;
        return time;
    }

    public static void vibrate(Context context, long milliseconds)
    {
        AndroidUtils.getVibrator(context).vibrate(
                VibrationEffect.createOneShot(
                        milliseconds,
                        VibrationEffect.DEFAULT_AMPLITUDE)
        );
    }

}
