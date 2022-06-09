package umu.software.activityrecognition.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.startup.AppInitializer;

import java.util.function.Consumer;

import umu.software.activityrecognition.config.PreferencesInitializers;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.RepeatingBroadcast;


/**
 * Helper class to start/stop/save using RecordService
 */
public class RecordServiceHelper
{
    public static final int RECURRENT_SAVE_REQUEST_CODE = 1001;

    private final Context mContext;

    RecordServiceHelper(Context context)
    {
        mContext = context;
    }

    public static RecordServiceHelper newInstance(Context context)
    {
        return new RecordServiceHelper(context.getApplicationContext());
    }


    /**
     * Start this service as a Service or ForegroundService depending on the system's version
     * @param intentBuilder builder for the starting intent. Used to set the extras
     */
    public void startRecording(@Nullable Consumer<Intent> intentBuilder)
    {
        Intent intent = newStartIntent();
        if (intentBuilder != null)
            intentBuilder.accept(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(mContext, intent);
        else
            mContext.startService(intent);
    }


    /**
     * Stops RecordService
     */
    public void stopRecording()
    {
        stopRecurrentSave();
        Intent intent = newStopIntent();
        mContext.startService(intent);
    }

    /**
     * Bind RecordService service with the given ServiceConnection
     * @param connection the ServiceConnection to use
     * @return whether the service got bound
     */
    public boolean bind(ServiceConnection connection)
    {
        Intent intent = new Intent(mContext, RecordService.class);
        return mContext.bindService(intent, connection, RecordService.BIND_AUTO_CREATE);
    }

    /**
     * Start the saveZipClearFiles() functionality of RecordService
     * @param zipPrefix the prefix to use for zip files. Can be null
     */
    public void saveZipClearFiles(@Nullable String zipPrefix)
    {
        Intent intent = newSaveIntent(zipPrefix);
        mContext.startService(intent);
    }

    /**
     * See saveZipClearFiles(String zipPrefix)
     */
    public void saveZipClearFiles()
    {
        saveZipClearFiles(null);
    }

    /**
     * Sets the classification label for the recordings
     */
    public void setSensorsLabel(String classification)
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_SET_CLASSIFICATION);
        intent.putExtra(RecordService.EXTRA_SENSOR_LABEL, classification);
        mContext.startService(intent);
    }



    /**
     * Start to recurrently save the sensor readings
     * @param intervalMillis milliseconds in between each save. Can be null to use the values from the preferences
     * @param zipPrefix the prefix to use for zip files. Can be null
     * @param intentBuilder builder for the intent recurrently broadcasted. Used to set the extras. Can be null
     */
    public void startRecurrentSave(@Nullable Long intervalMillis, @Nullable String zipPrefix, @Nullable Consumer<Intent> intentBuilder)
    {
        stopRecurrentSave();
        Intent saveIntent = newSaveIntent(zipPrefix);
        if (intentBuilder != null)
            intentBuilder.accept(saveIntent);
        PendingIntent pendingIntent = PendingIntent.getService(
                mContext,
                RECURRENT_SAVE_REQUEST_CODE,
                saveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (intervalMillis == null)
            intervalMillis = AppInitializer.getInstance(mContext).initializeComponent(PreferencesInitializers.RecordingsPreferencesInitializer.class).saveIntervalMillis();


        AndroidUtils.getAlarmManager(mContext).setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
        );

    }

    /**
     * Start recurrent save using the values from the preferences
     */
    public void startRecurrentSave()
    {
        startRecurrentSave(null, null, null);
    }

    /**
     * Stops recurrently sending intents to save the sensor readings
     */
    public void stopRecurrentSave()
    {
        Intent saveIntent = newSaveIntent(null);
        PendingIntent pendingIntent = PendingIntent.getService(
                mContext,
                RECURRENT_SAVE_REQUEST_CODE,
                saveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AndroidUtils.getAlarmManager(mContext).cancel(pendingIntent);
    }



    /**
     * Get the START_RECORDING intent with default parameters
     * @return the starting intent
     */
    public Intent newStartIntent()
    {
        Intent intent =  new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_START_RECORDING);
        return intent;
    }


    public Intent newStopIntent()
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_STOP_RECORDING);
        return intent;
    }




    public Intent newSaveIntent(@Nullable String zipPrefix)
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_SAVE_ZIP_CLEAR);
        if (zipPrefix != null)
            intent.putExtra(RecordService.EXTRA_ZIP_PREFIX, zipPrefix);
        return intent;
    }
}
