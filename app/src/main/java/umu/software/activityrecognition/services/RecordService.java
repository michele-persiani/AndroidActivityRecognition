package umu.software.activityrecognition.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;

import android.hardware.Sensor;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.concurrent.Callable;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.common.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.common.lifecycles.LifecyclesService;
import umu.software.activityrecognition.broadcastreceivers.RecurrentSave;
import umu.software.activityrecognition.data.classification.UserSpeechPrompts;
import umu.software.activityrecognition.data.persistence.Persistence;
import umu.software.activityrecognition.data.accumulators.SensorAccumulators;
import umu.software.activityrecognition.data.accumulators.SensorAccumulatorManager;
import umu.software.activityrecognition.speech.ASR;


/**
 * Service that records and saves sensor data through a SensorAccumulatorManager
 *
 * It offers the following actions to be set in the Intent:
 *  - RecordService.ACTION_START_RECORDING will start the sensor accumulators.
 *  - RecordService.ACTION_STOP_RECORDING will shut down the accumulators.
 *  - RecordService.ACTION_SAVE_TO_FILE will save the accumulator and zip them in an incremental way
 *    by utilizing the Persistence class
 *  - RecordService.ACTION_PROMPT_USER_CLASSIFICATION will prompt the user to classify the activity he's performing.
 *    The classification will be put together the sensor readings.
 *  - RecordService.ACTION_CLEAR_USER_CLASSIFICATION will clear the activity classification provided by the user.
 *
 *  The service also allows binding to directly access its methods
 *
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

    public static final String ACTION_START_RECORDING       = "RecordService.ACTION_START_RECORDING";
    public static final String ACTION_STOP_RECORDING        = "RecordService.ACTION_STOP_RECORDING";
    public static final String ACTION_SAVE_ZIP_CLEAR        = "RecordService.ACTION_SAVE_ZIP_CLEAR";
    public static final String ACTION_PROMPT_CLASSIFICATION = "RecordService.ACTION_PROMPT_CLASSIFICATION";
    public static final String ACTION_CLEAR_CLASSIFICATION  = "RecordService.ACTION_CLEAR_CLASSIFICATION";


    public static final String EXTRA_RECURRENT_SAVE_SECS = "RecordService.EXTRA_RECURRENT_SAVE_SECS";
    public static final int DEFAULT_RECURRENT_SAVE_SECS = 900;

    public static final String EXTRA_RECORDED_SENSOR_TYPES = "RecordService.EXTRA_RECORDED_SENSOR_TYPES";
    public static final int[] DEFAULT_RECORDED_SENSOR_TYPES = new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE};

    public static final String EXTRA_MIN_DELAY_MILLIS = "RecordService.EXTRA_MIN_DELAY_MILLIS";
    public static final long DEFAULT_MIN_DELAY_MILLIS = 50;


    SensorAccumulatorManager mAccumulatorManager;

    ForegroundServiceLifecycle mForegroundLifecycle;



    @SuppressLint("LaunchActivityFromNotification")
    @Override
    public void onCreate()
    {
        super.onCreate();
        mForegroundLifecycle = new ForegroundServiceLifecycle(
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


        addLifecycleElement(mForegroundLifecycle);
        addLifecycleElement(UserSpeechPrompts.USER_ACTIVITY_CLASSIFICATION);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        Log.i(RecordService.class.getName(),
                String.format("RecordService: onStartCommand() -> %s", (intent != null)? intent.toString() : "null")
        );

        String action = intent == null? ACTION_START_RECORDING : intent.getAction();

        switch (action)
        {
            case RecordService.ACTION_START_RECORDING:
                startRecording(intent);
                break;
            case RecordService.ACTION_STOP_RECORDING:
                stopSelf();
                break;
            case RecordService.ACTION_SAVE_ZIP_CLEAR:
                saveZipClearSensorsFiles();
                break;
            case RecordService.ACTION_PROMPT_CLASSIFICATION:
                requestUserClassification();
                break;
            case RecordService.ACTION_CLEAR_CLASSIFICATION:
                clearUserClassification();
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
        RecurrentSave.stopRecurrentSave(this);
        saveZipClearSensorsFiles();
        stopForeground(true);
        Log.i(getClass().getSimpleName(), "Service destroyed");
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return new RecordBinder(this);
    }


    /**
     * Start recording sensor data
     * @param intent the intent requesting to start recording, or null. If intent is null we use the default values
     */
    public void startRecording(Intent intent)
    {
        if(mAccumulatorManager != null)
            removeLifecycleElement(mAccumulatorManager);

        int[] sensorTypes = intent == null? DEFAULT_RECORDED_SENSOR_TYPES : intent.getIntArrayExtra(EXTRA_RECORDED_SENSOR_TYPES);
        long minDelayMillis = intent == null? DEFAULT_MIN_DELAY_MILLIS : intent.getLongExtra(EXTRA_MIN_DELAY_MILLIS, DEFAULT_MIN_DELAY_MILLIS);
        int saveDelay = intent == null? DEFAULT_RECURRENT_SAVE_SECS : intent.getIntExtra(EXTRA_RECURRENT_SAVE_SECS, DEFAULT_RECURRENT_SAVE_SECS);


        if (sensorTypes.length == 0)
        {
            Log.w(getClass().getSimpleName(), "Number of sensors in zero. Did you forget to set the intent extra?");
            Log.w(getClass().getSimpleName(), "Stopping self.");
            stopSelf();
            return;
        }

        mAccumulatorManager = SensorAccumulators.newAccumulatorManager(
                SensorAccumulators.newFactory((acc) -> {
                    acc.consumers().add((sensorEvent, row) -> {
                        String activity = UserSpeechPrompts.USER_ACTIVITY_CLASSIFICATION.getLastAnswer();
                        row.put("classification", activity);
                    });
                    acc.setMinDelayMillis(minDelayMillis);
                }), false, sensorTypes
        );
        addLifecycleElement(mAccumulatorManager);

        RecurrentSave.stopRecurrentSave(this);
        if (saveDelay > 0)
            RecurrentSave.startRecurrentSave(this, saveDelay);
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
        Callable<Integer> zip    = createIncrementalZipSensorsFiles();
        Callable<Integer> delete = clearSensorsFiles();
        return () -> {
            Integer[] result = new Integer[3];
            result[0] = save.call();
            result[1] = zip.call();
            result[2] = delete.call();
            return result;
        };
    }


    /**
     * Save the accumulated readings to corresponding files. Each file takes the name of the corresponding sensor.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer> saveSensorsFiles()
    {
        return Persistence.DOCUMENTS_FOLDER.saveToFile(mAccumulatorManager.getAccumulators().values(), true);
    }


    /**
     * Zip the previously saved files relative to the sensor accumulators. Each time this method is called
     * the name of the zip is set in an incremental way: 0.zip, 1.zip, 2.zip, ...  depending on the
     * numbered zips present in the folder
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer> createIncrementalZipSensorsFiles()
    {
        return Persistence.DOCUMENTS_FOLDER.createIncrementalZip(mAccumulatorManager.getAccumulators().values());
    }


    /**
     * Clear the files associated with the previously started sensor accumulators.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called
     */
    public Callable<Integer> clearSensorsFiles()
    {
        return Persistence.DOCUMENTS_FOLDER.deleteFiles(mAccumulatorManager.getAccumulators().values());
    }



    /**
     * Delete the folder where the sensor readings have been saved
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called
     */
    public Callable<Integer> deleteSaveFolder()
    {
        return Persistence.DOCUMENTS_FOLDER.deleteSaveFolder();
    }


    /**
     * Requests the user to classify the activity he's performing using speech. The recorded speech
     * will be appended to the sensor dataframes
     */
    public void requestUserClassification()
    {
        UserSpeechPrompts.USER_ACTIVITY_CLASSIFICATION.prompt();
    }

    /**
     * Clear the last classification of the activity provided by the user
     */
    public void clearUserClassification()
    {
        UserSpeechPrompts.USER_ACTIVITY_CLASSIFICATION.clearLastAnswer();
    }





    /*     Helper functions     */


    /**
     * Get the START_RECORDING intent with the specified save interval
     * @param context calling android context
     * @param recurrentSaveSecs the seconds to recurrently invoke the save and zip procedure
     * @return the starting intent
     */
    public static Intent getStartIntent(Context context, int recurrentSaveSecs, long minDelayMillis, int... sensorTypes)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_START_RECORDING);
        intent.putExtra(EXTRA_RECURRENT_SAVE_SECS, recurrentSaveSecs);
        intent.putExtra(EXTRA_RECORDED_SENSOR_TYPES, sensorTypes);
        intent.putExtra(EXTRA_MIN_DELAY_MILLIS, minDelayMillis);
        return intent;
    }


    public static Intent getStopIntent(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        return intent;
    }



    /**
     * Start this service as a Service or ForegroundService depending on the system's version
     * @param context the calling Android context
     * @param recurrentSaveSecs the seconds to recurrently invoke the save and zip procedure
     * @return the service's ComponentName
     */
    public static ComponentName startRecording(Context context, int recurrentSaveSecs, long minDelayMillis, int... sensorTypes)
    {
        Intent intent = RecordService.getStartIntent(context, recurrentSaveSecs, minDelayMillis, sensorTypes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            ContextCompat.startForegroundService(context, intent);
            return new ComponentName(context, RecordService.class);
        }
        else
            return context.startService(intent);
    }

    /**
     * Stops the service
     * @param context the calling context
     * @return whether the service got stop
     */
    public static ComponentName stop(Context context)
    {
        Intent intent = getStopIntent(context);
        return context.startService(intent);
    }

    /**
     * Bind the service with the given ServiceConnection
     * @param context the calling context
     * @param connection the ServiceConnection to use
     * @return whether the service got bound
     */
    public static boolean bind(Context context, ServiceConnection connection)
    {
        Intent intent = new Intent(context, RecordService.class);
        return context.bindService(intent, connection, RecordService.BIND_AUTO_CREATE);
    }

    /**
     * Start the saveZipClearFiles() functionality of the service
     * @param context the calling Android context
     * @return the ComponentName of the service
     */
    public static ComponentName saveZipClearFiles(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(RecordService.ACTION_SAVE_ZIP_CLEAR);
        return context.startService(intent);
    }

    /**
     * Request the user to classify its crrent activity
     * @param context th calling context
     * @return
     */
    public static ComponentName requestUserClassification(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(RecordService.ACTION_PROMPT_CLASSIFICATION);
        return context.startService(intent);
    }


    /**
     * Clear the previously given activity classification of the user
     * @param context the calling context
     * @return
     */
    public static ComponentName clearUserClassification(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(RecordService.ACTION_CLEAR_CLASSIFICATION);
        return context.startService(intent);
    }

    /**
     * Asks permissions for this service
     * @param activity the calling activity
     */
    public static void askPermissions(Activity activity)
    {
        Persistence.DOCUMENTS_FOLDER.askPermissions(activity);
        ASR.FREE_FORM.askPermissions(activity);
    }
}