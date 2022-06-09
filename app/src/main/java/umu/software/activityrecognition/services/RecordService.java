package umu.software.activityrecognition.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;

import android.hardware.Sensor;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.startup.AppInitializer;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.config.PreferencesInitializers;
import umu.software.activityrecognition.config.RecordingsPreferences;
import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.AccumulatorsFactory;
import umu.software.activityrecognition.data.accumulators.AccumulatorsLifecycle;
import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.lifecycles.LifecycleDelegateObserver;
import umu.software.activityrecognition.shared.lifecycles.LifecycleStartedService;
import umu.software.activityrecognition.shared.lifecycles.WakeLockLifecycle;
import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.persistence.Persistence;
import umu.software.activityrecognition.tflite.TFLiteNamedModels;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;
import umu.software.activityrecognition.tflite.model.TFModel;


/**
 * Service that records and saves sensor data through a SensorAccumulatorManager
 *
 * It offers the following actions to be set in the Intent:
 *  - ACTION_START_RECORDING will start the sensor accumulators
 *  - ACTION_STOP_RECORDING will shut down the accumulators
 *  - ACTION_SAVE_TO_FILE will save the accumulator and zip them in an incremental way
 *    by utilizing the Persistence class
 *  - ACTION_SET_CLASSIFICATION will prompt the user to classify the activity he's performing
 *    The classification will be put together the sensor readings
 *
 *  The service also allows binding to directly access its methods
 *
 */
public class RecordService extends LifecycleStartedService
{

    public static class RecordServiceBinder extends LocalBinder<RecordService>
    {
        public RecordServiceBinder(RecordService service){
            super(service);
        }
    }

    /** Starts recording from sensors and models */
    public static final String ACTION_START_RECORDING             = "ACTION_START_RECORDING";
    /** EXTRA_RECORDED_SENSORS is set with intent.setArrayListExtra() and is the list sensor names */
    public static final String EXTRA_RECORDED_SENSORS             = "RECORDED_SENSORS";
    public static final String EXTRA_SENSORS_MIN_DELAY_MILLIS     = "SENSORS_MIN_DELAY_MILLIS";
    public static final String EXTRA_MODELS_MIN_DELAY_MILLIS      = "MODELS_MIN_DELAY_MILLIS";
    public static final String EXTRA_USE_WAKE_LOCK                = "USE_WAKE_LOCK";

    /** Saves the currently gathered recordings */
    public static final String ACTION_SAVE_ZIP_CLEAR              = "ACTION_SAVE_ZIP_CLEAR";
    public static final String EXTRA_ZIP_PREFIX                   = "ZIP_PREFIX";

    /** Sets the recording's label */
    public static final String ACTION_SET_CLASSIFICATION          = "ACTION_SET_CLASSIFICATION";
    public static final String EXTRA_SENSOR_LABEL                 = "SENSOR_CLASSIFICATION";

    /** Stops the recordings */
    public static final String ACTION_STOP_RECORDING              = "ACTION_STOP_RECORDING";


    private LifecycleObserver mWakeLockLifecycle;

    private AccumulatorsLifecycle mAccumulators;

    private boolean mRecording;

    private String mLabel = null;


    public boolean isRecording()
    {
        return mRecording;
    }

    @SuppressLint("LaunchActivityFromNotification")
    @Override
    public void onCreate()
    {
        super.onCreate();
        mRecording = false;
        Intent stopIntent = new Intent(this, RecordService.class).setAction(ACTION_STOP_RECORDING);

        PendingIntent pendingIntent = PendingIntent.getService(
                this,
                10045,
                stopIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        ForegroundServiceLifecycle foregroundLifecycle = new ForegroundServiceLifecycle(
                this,
                builder -> {
                    builder.setContentTitle(getString(R.string.notification_title))
                            .setContentText(getString(R.string.notification_text))
                            .setSmallIcon(R.mipmap.ic_watch_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setVibrate(new long[]{0L, 0L, 0L});
                }
        );
        getLifecycle().addObserver(foregroundLifecycle);

        mWakeLockLifecycle = WakeLockLifecycle.newPartialWakeLock(this);


        mAccumulators = new AccumulatorsLifecycle();
        getLifecycle().addObserver(new LifecycleDelegateObserver(mAccumulators.getLifecycle()));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        logInfo("RecordService: onStartCommand() -> %s", (intent != null)? intent.toString() : "null");


        String action = intent == null? ACTION_START_RECORDING : intent.getAction();

        switch (action)
        {
            case ACTION_START_RECORDING:
                startRecording(intent);
                resumeLifecycle();
                break;
            case ACTION_STOP_RECORDING:
                pauseLifecycle();
                stopRecording(intent);
                break;
            case ACTION_SAVE_ZIP_CLEAR:
                saveRecording(intent);
                break;
            case ACTION_SET_CLASSIFICATION:
                setLabel(intent);
                break;
            default:
                logInfo("RecordService: unknown Action -> %s", intent);
                break;
        }
        if (!isRecording())
            stopSelf();
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mRecording = false;
        logInfo("Service destroyed");
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return new RecordServiceBinder(this);
    }


    /**
     * Start recording sensor data
     * @param intent the intent requesting to start recording, or null.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         If intent is null we use the default values
     */
    private void startRecording(Intent intent)
    {
        if(isRecording())
            return;

        RecordingsPreferences preferences = AppInitializer
                .getInstance(this)
                .initializeComponent(PreferencesInitializers.RecordingsPreferencesInitializer.class);

        boolean useWakeLock = intent.getBooleanExtra(
                EXTRA_USE_WAKE_LOCK,
                preferences.useWakeLock()
        );
        List<String> allowedSensors = (intent.hasExtra(EXTRA_RECORDED_SENSORS))? intent.getStringArrayListExtra(EXTRA_RECORDED_SENSORS) : Lists.newArrayList();

        long sensorsMinDelayMillis = intent.getLongExtra(
                EXTRA_SENSORS_MIN_DELAY_MILLIS,
                preferences.sensorsReadingsDelayMillis()
        );
        long modelsMinDelayMillis = intent.getLongExtra(
                EXTRA_MODELS_MIN_DELAY_MILLIS,
                preferences.modelsReadingsDelayMillis()
        );



        List<Sensor> sensors = AndroidUtils.getSensorManager(this).getSensorList(Sensor.TYPE_ALL);
        AccumulatorsFactory factory = AccumulatorsFactory.newInstance(this);
        Predicate<Sensor> recordSensor = preferences.getRecordSensorPredicate();
        Predicate<Sensor> combinedFilter = (s) -> allowedSensors.contains(s.getName()) || recordSensor.test(s);

        List<String> recordedSensors = Lists.newArrayList();

        sensors.stream().filter(combinedFilter).forEach((s) -> {
            recordedSensors.add(s.getName());
            Accumulator<?> accum = factory.newSensor(s, (acc) -> {
                acc.setMinDelayMillis(sensorsMinDelayMillis);
                acc.consumers().add((sensorEvent, row) -> {
                    row.put("label", mLabel);
                });
            });
            mAccumulators.put(s, accum);
        });



        List<AccumulatorTFModel> recordedModels = Lists.newArrayList(
                TFLiteNamedModels.SOM.newInstance(this)
        );
        List<String> modelNames = recordedModels.stream().map(TFModel::getName).collect(Collectors.toList());

        for (AccumulatorTFModel model : recordedModels)
        {
            // Add SOM
            Object somKey = new Object();

            mAccumulators.getLifecycle().addObserver(new LifecycleDelegateObserver(model.getLifecycle()));

            Accumulator<?> accum = factory.newTFModel(model, (acc) -> {
                acc.setMinDelayMillis(modelsMinDelayMillis);
                acc.consumers().add((event, row) -> {
                    String activity = mLabel;
                    row.put("label", activity);
                });
            });
            mAccumulators.put(somKey, accum);
        }

        logInfo("Recording parameters.\nUse WakeLock (%s)\nRecorded sensors (%s)\nSensors delay (%s)\nRecorded models (%s)\nModels delay (%s)",
                useWakeLock,
                String.join(", ", recordedSensors),
                sensorsMinDelayMillis,
                String.join(", ", modelNames),
                modelsMinDelayMillis
        );

        if (useWakeLock) getLifecycle().addObserver(mWakeLockLifecycle);
        mRecording = true;
    }

    /**
     * Stop recording from sensors
     * @param intent the calling intent
     */
    private void stopRecording(Intent intent)
    {
        if (!isRecording())
            return;
        stopSelf();
    }

    /**
     * Save the currently gathered sensor readings through saveZipClearSensorsFiles().
     * @param intent the calling intent. The extra EXTRA_ZIP_PREFIX can be set to select the zip prefix
     */
    private void saveRecording(Intent intent)
    {
        if (!isRecording())
            return;
        String filePrefix = intent.hasExtra(EXTRA_ZIP_PREFIX)? intent.getStringExtra(EXTRA_ZIP_PREFIX) : null;
        saveZipClearSensorsFiles(filePrefix);

        // Notify user through vibration
        AndroidUtils.getVibrator(this).vibrate(
                VibrationEffect.createOneShot(
                        500,
                        VibrationEffect.DEFAULT_AMPLITUDE)
        );
    }



    /**
     * Sets the label for the sensor readings. The classification
     * will be appended to the sensor dataframes
     * @param intent the intent requesting the change of classification. Should contain the extra
     *               EXTRA_SENSOR_CLASSIFICATION
     */
    public void setLabel(Intent intent)
    {
        if(!isRecording())
            return;
        if (intent.hasExtra(EXTRA_SENSOR_LABEL))
        {
            mLabel = intent.getStringExtra(EXTRA_SENSOR_LABEL);
            logInfo("Setting sensors label to: %s", mLabel);
        }
        else
            mLabel = null;
    }


    /**
     * Fetches dataframes from the accumulator manager
     * @return collection of dataframes
     */
    public Collection<DataFrame> getDataFrames()
    {
        return mAccumulators.getDataFrames();
    }

    /**
     * Clear accumulated dataframes
     */
    public void clearDataFrames()
    {
        mAccumulators.clearDataFrames();
    }


    /**
     * Perform saving, compression, and clearing files in succession.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * However, the method executes even if the call() method in not invoked.
     * @param zipPrefix The prefix to use in the incremental zip. The zips are incrementally saved as
     *                  'prefix.N.zip' where N is an incremental value depending on the amount of previously saved zips.
     *                  zipPrefix can be null and in this case it will be ignored and zips will be named 'N.zip'.
     * @return A Callable to access the result of the operation.
     * The operation performs even if this method is not called.
     */
    public Callable<Integer[]> saveZipClearSensorsFiles(String zipPrefix)
    {
        Collection<DataFrame> dataframes = getDataFrames();
        clearDataFrames();
        Callable<Integer> save   = Persistence.SENSORS_FOLDER.saveToFile(dataframes);
        Callable<Integer> zip    = Persistence.SENSORS_FOLDER.createIncrementalZip(zipPrefix, dataframes);
        Callable<Integer> delete = Persistence.SENSORS_FOLDER.deleteFiles(dataframes);

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
     * !! The operation performs even if the call() method is not called !!
     */
    public Callable<Integer> saveSensorsFiles()
    {
        return Persistence.SENSORS_FOLDER.saveToFile(getDataFrames());
    }


    /**
     * Zip the previously saved files relative to the sensor accumulators. Each time this method is called
     * the name of the zip is set in an incremental way: prefix.0.zip, prefix.1.zip, prefix.2.zip, ...  depending on the
     * numbered zips present in the folder
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @param prefix the prefix for the zip file. Can be null.
     * @return A Callable to access the result of the operation.
     * !! The operation performs even if the call() method is not called !!
     */
    public Callable<Integer> createIncrementalZipSensorsFiles(String prefix)
    {
        return Persistence.SENSORS_FOLDER.createIncrementalZip(prefix, getDataFrames());
    }


    /**
     * Clear the files associated with the previously started sensor accumulators.
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * !! The operation performs even if the call() method is not called !!
     */
    public Callable<Integer> clearSensorsFiles()
    {
        return Persistence.SENSORS_FOLDER.deleteFiles(getDataFrames());
    }



    /**
     * Delete the folder where the sensor readings have been saved
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * !! The operation performs even if the call() method is not called !!
     */
    public Callable<Integer> deleteSaveFolder()
    {
        return Persistence.SENSORS_FOLDER.deleteSaveFolder();
    }

    /**
     * Helper logger function
     * @param formatted
     * @param args
     */
    private void logInfo(String formatted, Object... args)
    {
        Log.i(getClass().getSimpleName(), String.format(formatted, args));
    }
}