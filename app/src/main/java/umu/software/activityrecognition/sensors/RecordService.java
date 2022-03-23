package umu.software.activityrecognition.sensors;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.concurrent.Callable;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.common.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.common.lifecycles.LifecyclesService;
import umu.software.activityrecognition.sensors.persistence.Persistence;
import umu.software.activityrecognition.sensors.accumulators.Accumulators;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulatorManager;


/**
 * Started Service that records and saves sensor data through a SensorAccumulatorManager
 * It has three possible actions to be set in the Intent:
 *  - RecordService.ACTION_START_RECORDING will start the sensor accumulators
 *  - RecordService.ACTION_STOP_RECORDING will shut down the accumulators
 *  - RecordService.ACTION_SAVE_TO_FILE will save the accumulator and zip them in an incremental way
 *    bt utilizing the Persistence class
 *  The service also allows binding to directly access its methods
 */
public class RecordService extends LifecyclesService
{
    public static class RecordBinder extends Binder
    {
        private final RecordService service;

        public RecordBinder(RecordService service){
            this.service = service;
        }

        public RecordService getService() {
            return this.service;
        }
    }


    public static final String ACTION_START_RECORDING = "RecordService.ACTION_START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "RecordService.ACTION_STOP_RECORDING";
    public static final String ACTION_SAVE_ZIP_CLEAR = "RecordService.ACTION_SAVE_TO_FILE";


    public static final String EXTRA_RECURRENT_SAVE_SECS = "RecordService.ACTION_ZIP_FILES";
    public static final int DEFAULT_RECURRENT_SAVE_SECS = 60;


    boolean mRestartOnDestroy = true;

    SensorAccumulatorManager mAccumulatorManager = Accumulators.newAccumulatorManager(Accumulators.newFactory());

    ForegroundServiceLifecycle foregroundLifecycle;



    @SuppressLint("LaunchActivityFromNotification")
    @Override
    public void onCreate()
    {
        super.onCreate();
        foregroundLifecycle = new ForegroundServiceLifecycle(
            140000,
            this.getString(R.string.notification_title),
            builder -> {
                Intent stopIntent = getStopIntent(this);

                PendingIntent pendingIntent = PendingIntent.getService(
                        this,
                        0,
                        stopIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                builder
                        .setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_text))
                        .setSmallIcon(R.mipmap.ic_watch_round)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setVibrate(new long[]{0L, 0L, 0L});
            }
        );
        addLifecycleElement(mAccumulatorManager);
        addLifecycleElement(foregroundLifecycle);
        mRestartOnDestroy = true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(RecordService.class.getName(),
                String.format("RecordService: onStartCommand() -> %s", (intent != null)? intent.toString() : "null")
        );
        super.onStartCommand(intent, flags, startId);

        String action = intent == null? ACTION_START_RECORDING : intent.getAction();

        switch (action)
        {
            case RecordService.ACTION_START_RECORDING: /* Since the accumulators are already handled by the lifecycle manager we only need to restart RecurrentSave */
                RecurrentSave.stop(this);
                int saveDelay = intent == null? DEFAULT_RECURRENT_SAVE_SECS : intent.getIntExtra(EXTRA_RECURRENT_SAVE_SECS, DEFAULT_RECURRENT_SAVE_SECS);
                RecurrentSave.start(this, saveDelay);
                break;
            case RecordService.ACTION_STOP_RECORDING:
                mRestartOnDestroy = false;
                saveZipClearSensorsFiles();
                RecurrentSave.stop(this);
                stopSelf();
                break;
            case RecordService.ACTION_SAVE_ZIP_CLEAR:
                saveZipClearSensorsFiles();
                break;
            default:
                Log.w(RecordService.class.getName(),
                        String.format("RecordService: unknown Action -> %s", intent)
                );
                break;
        }

        return START_STICKY;
    }


    /**
     * Restarts the service if it was not destroyed through an intent but by the system
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        saveZipClearSensorsFiles();
        if (mRestartOnDestroy)
        {
            RecordServiceStarter.broadcast(this);
        }
    }



    @Override
    public IBinder onBind(Intent intent)
    {
        return new RecordBinder(this);
    }






    /**
     * Perform saving, compression, and clearing files in succession.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer[]> saveZipClearSensorsFiles()
    {
        Callable<Integer> save   = saveSensorsFiles();
        Callable<Integer> zip    = incrementalZipSensorsFiles();
        Callable<Integer> delete = clearSensorsFiles();
        return () -> new Integer[]{save.call(), zip.call(), delete.call()};
    }


    /**
     * Save the accumulated readings to corresponding files. Each file takes the name of the corresponding sensor.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer> saveSensorsFiles()
    {
        return Persistence.INSTANCE.saveToFile(mAccumulatorManager.getAccumulators().values(), true);
    }


    /**
     * Zip the previously saved files relative to the sensor accumulators. Each time this method is called
     * the name of the zip is set in an incremental way: 0.zip, 1.zip, 2.zip, ...  depending on the
     * numbered zips present in the filder
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer> incrementalZipSensorsFiles()
    {
        return Persistence.INSTANCE.createIncrementalZip(mAccumulatorManager.getAccumulators().values());
    }


    /**
     * Clear the files associated with the previously started sensor accumulators.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called
     */
    public Callable<Integer> clearSensorsFiles()
    {
        return Persistence.INSTANCE.deleteFiles(mAccumulatorManager.getAccumulators().values());
    }



    /**
     * Delete the folder where the sensor readings have been saved
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called
     */
    public Callable<Integer> deleteSaveFolder()
    {
        return Persistence.INSTANCE.deleteSaveFolder();
    }




    // Helper functions to start the service

    public static Intent getStartIntent(Context context)
    {
        return getStartIntent(context, DEFAULT_RECURRENT_SAVE_SECS);
    }

    /**
     * Get the START_RECORDING intent with the specified save interval
     * @param context calling android context
     * @param recurrentSaveSecs the seconds to recurrently invoke the save and zip procedure
     * @return the starting intent
     */
    public static Intent getStartIntent(Context context, int recurrentSaveSecs)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_START_RECORDING);
        intent.putExtra(EXTRA_RECURRENT_SAVE_SECS, recurrentSaveSecs);
        return intent;
    }


    public static Intent getStopIntent(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        return intent;
    }

    /**
     * Start this service as a Service or ForegroundService depending on the system's version.
     * Use DEFAULT_RECURRENT_SAVE_SECS as seconds for the recurrent save
     * @param context the calling Android context
     * @return the service's ComponentName
     */
    public static ComponentName start(Context context)
    {
        return start(context, DEFAULT_RECURRENT_SAVE_SECS);
    }



    /**
     * Start this service as a Service or ForegroundService depending on the system's version
     * @param context the calling Android context
     * @param recurrentSaveSecs the seconds to recurrently invoke the save and zip procedure
     * @return the service's ComponentName
     */
    public static ComponentName start(Context context, int recurrentSaveSecs)
    {
        Intent intent = getStartIntent(context, recurrentSaveSecs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            ContextCompat.startForegroundService(context, getStartIntent(context));
            return new ComponentName(context, RecordService.class);
        }
        return context.startService(intent);
    }

    /**
     * Stops the service
     * @param context
     * @return whether the service got stop
     */
    public static boolean stop(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        return context.stopService(intent);
    }

    /**
     * Bind the service with the given ServiceConnection
     * @param context
     * @param connection
     * @return whether the service got bound
     */
    public static boolean bind(Context context, ServiceConnection connection)
    {
        Intent intent = new Intent(context, RecordService.class);
        return context.bindService(intent, connection, BIND_AUTO_CREATE);
    }

    /**
     * Start the saveZipClearFiles() functionality of the service
     * @param context the calling Android context
     * @return the ComponentName of the service
     */
    public static ComponentName saveZipClearFiles(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_SAVE_ZIP_CLEAR);
        return context.startService(intent);
    }


}