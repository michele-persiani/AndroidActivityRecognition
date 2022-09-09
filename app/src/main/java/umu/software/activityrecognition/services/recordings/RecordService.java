package umu.software.activityrecognition.services.recordings;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;

import android.hardware.Sensor;
import android.os.IBinder;
import android.os.VibrationEffect;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.startup.AppInitializer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.data.dataframe.RowParcelable;
import umu.software.activityrecognition.preferences.RecordServicePreferences;
import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.AccumulatorsFactory;
import umu.software.activityrecognition.data.accumulators.AccumulatorsLifecycle;
import umu.software.activityrecognition.preferences.initializers.RecordingsPreferencesInitializer;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.RepeatingBroadcast;
import umu.software.activityrecognition.shared.util.UniqueId;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.lifecycles.LifecycleDelegateObserver;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.lifecycles.WakeLockLifecycle;
import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.persistence.Persistence;
import umu.software.activityrecognition.tflite.TFLiteNamedModels;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;


/**
 * Service that records and saves sensor data through a SensorAccumulatorManager
 *
 * It offers the following actions to be set in the Intent:
 *  - ACTION_START_RECORDING will start the sensor accumulators
 *  - ACTION_STOP_RECORDING will shut down the accumulators
 *  - ACTION_SAVE_TO_FILE will save the accumulator and zip them in an incremental way
 *    by utilizing the Persistence class
 *  - ACTION_SET_CLASSIFICATION sets the classification for next recordings until its sets again or reset
 *    The classification will be put in the recordings dataframes
 *  - ACTION_SEND_EVENT allows to store dataframe rows in accumulators registered as BroadcastReceivers
 *
 *  Allows to register and manage external accumulators through setupBroadcastReceiver(),
 *  removeBroadcastReceiver() and clearBroadcastReceivers()
 *
 *
 *  The service also allows binding to directly access its methods
 *
 *
 */
public class RecordService extends LifecycleService
{
    public static class Binder extends ServiceBinder<RecordService>
    {
        public Binder(RecordService service){
            super(service);
        }
    }


    /** Starts automatic recurrent save */
    public static final String ACTION_START_RECURRENT_SAVE        = "umu.software.activityrecognition.ACTION_START_RECURRENT_SAVE";

    /** Stops automatic recurrent save */
    public static final String ACTION_STOP_RECURRENT_SAVE         = "umu.software.activityrecognition.ACTION_STOP_RECURRENT_SAVE";

    /** Starts recording from sensors and models */
    public static final String ACTION_START_RECORDING             = "umu.software.activityrecognition.ACTION_START_RECORDING";

    /** Stops the recordings */
    public static final String ACTION_STOP_RECORDING              = "umu.software.activityrecognition.ACTION_STOP_RECORDING";


    /** Saves the currently gathered recordings */
    public static final String ACTION_SAVE_ZIP_CLEAR              = "umu.software.activityrecognition.ACTION_SAVE_ZIP_CLEAR";


    /** Sets the recording's label */
    public static final String ACTION_SET_CLASSIFICATION          = "umu.software.activityrecognition.ACTION_SET_CLASSIFICATION";
    /** String describing the label */
    public static final String EXTRA_SENSOR_LABEL                 = "SENSOR_CLASSIFICATION";

    /** Used to record broadcasted events
     * Broadcasted events are recorded after calling setupBroadcastReceiver()
     * */
    public static final String ACTION_RECORD_EVENT                = "umu.software.activityrecognition.ACTION_RECORD_EVENT";
    /** String identifying the accumulator*/
    public static final String EXTRA_ACCUMULATOR_ID               = "ACCUMULATOR_ID";
    /** RowParcelable describing the row to add */
    public static final String EXTRA_EVENT                        = "EVENT";


    private LifecycleObserver mWakeLockLifecycle;
    private ForegroundServiceLifecycle mFregroundLifecycle;
    private AccumulatorsLifecycle mAccumulators;
    private RepeatingBroadcast mRepeatingBroadcast;
    private RecordServicePreferences mPreferences;
    private Binder mBinder;
    private boolean mRecording = false;
    private String mLabel = null;

    private final Map<String, BroadcastReceiver> mBroadcastReceivers = Maps.newHashMap();




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
        mRepeatingBroadcast = new RepeatingBroadcast(this);
        mAccumulators = new AccumulatorsLifecycle();
        getLifecycle().addObserver(new LifecycleDelegateObserver(mAccumulators.getLifecycle()));
        mPreferences = AppInitializer
                .getInstance(this)
                .initializeComponent(RecordingsPreferencesInitializer.class);

        mWakeLockLifecycle = WakeLockLifecycle.newPartialWakeLock(this);

        PendingIntent closePendingIntent = PendingIntent.getService(
                this,
                10045,
                new Intent(this, RecordService.class).setAction(ACTION_STOP_RECORDING),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Action closeAction = new NotificationCompat.Action.Builder(null, getString(R.string.close), closePendingIntent).build();

        mFregroundLifecycle = new ForegroundServiceLifecycle(
                this,
                UniqueId.uniqueInt(),
                getString(R.string.notification_channel_id),
                builder -> {
                    builder.setContentTitle(getString(R.string.recording_notification_title))
                            .setContentText(getString(R.string.recording_notification_text))
                            .setSmallIcon(R.mipmap.ic_watch_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                            .addAction(closeAction)
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setGroup(getString(R.string.notification_group_id));
                }
        );


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        String action = intent == null? ACTION_START_RECORDING : intent.getAction();

        switch (action)
        {
            case ACTION_START_RECORDING:
                startRecording(intent);
                mRecording = true;
                break;
            case ACTION_STOP_RECORDING:
                mRecording = false;
                break;
            case ACTION_START_RECURRENT_SAVE:
                startRecurrentSave(intent);
                break;
            case ACTION_STOP_RECURRENT_SAVE:
                stopRecurrentSave();
                break;
            case ACTION_SAVE_ZIP_CLEAR:
                saveRecording(intent);
                break;
            case ACTION_SET_CLASSIFICATION:
                setLabel(intent);
                break;
            default:
                logger().i("RecordService: unknown Action -> %s", intent);
                break;
        }
        if (!isRecording())
            stopRecording(null);
        return START_REDELIVER_INTENT;
    }

    private void stopRecording(@Nullable Intent intent)
    {
        stopRecurrentSave();
        stopForeground(true);
        stopSelf();
        mAccumulators.clear();
        mPreferences.clearListeners();
        mRecording = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        logger().i("Cleared (%s) broadcast receivers", clearBroadcastReceivers());
        logger().i("Service destroyed");
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        if (mBinder == null)
            mBinder = new Binder(this);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    /**
     * Start recording sensor data
     * @param intent the intent requesting to start recording.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         If intent is null we use the default values
     */
    private void startRecording(@Nullable Intent intent)
    {
        if(isRecording())
            return;

        mPreferences.clearListeners();
        mAccumulators.clear();

        Preference<Boolean> useWakeLock = mPreferences.useWakeLock();
        Preference<Integer> sensorsMinDelayMillis = mPreferences.sensorsReadingsDelayMillis();
        Preference<Integer> modelsMinDelayMillis = mPreferences.modelsReadingsDelayMillis();
        Predicate<Sensor> recordSensor = mPreferences.recordSensorPredicate();
        Predicate<TFLiteNamedModels> recordModel = mPreferences.recordModelPredicate();

        Map<Object, Consumer<Preference<Integer>>> preferenceListeners = Maps.newHashMap();


        /* Add sensors accumulators */
        List<Sensor> sensors = AndroidUtils.getSensorManager(this).getSensorList(Sensor.TYPE_ALL);
        List<String> recordedSensorNames = Lists.newArrayList();


        AccumulatorsFactory factory = AccumulatorsFactory.newInstance(this);

        for (Sensor s : sensors)
        {
            if (recordSensor.test(s))
            {
                registerSensor(s, factory, preferenceListeners);
                recordedSensorNames.add(s.getName());
            }
            mPreferences.recordSensor(s).registerListener( p -> {
                if (p.get())
                    registerSensor(s, factory, preferenceListeners);
                else
                    unregisterSensor(s, preferenceListeners);
            });
        }

        /* Add models accumulators */
        List<String> modelNames = Lists.newArrayList();

        for (TFLiteNamedModels namedModel : TFLiteNamedModels.values())
        {
            AccumulatorTFModel model = namedModel.newInstance(this);

            if (recordModel.test(namedModel))
            {
                registerModel(model, factory, preferenceListeners);
                modelNames.add(model.getName());
            }

            mPreferences.recordModel(model.getName()).registerListener( p -> {
                if (p.get())
                    registerModel(model, factory, preferenceListeners);
                else
                    unregisterModel(model, preferenceListeners);
            });

        }

        if (useWakeLock.get()) getLifecycle().addObserver(mWakeLockLifecycle);
        getLifecycle().removeObserver(mFregroundLifecycle);
        getLifecycle().addObserver(mFregroundLifecycle);


        logger().i("Recording parameters.\nUse WakeLock (%s)\nRecorded sensors (%s)\nSensors delay (%s)\nRecorded models (%s)\nModels delay (%s)",
                useWakeLock.get(),
                String.join(", ", recordedSensorNames),
                sensorsMinDelayMillis.get(),
                String.join(", ", modelNames),
                modelsMinDelayMillis.get()
        );
    }


    /* ----------- START Helper functions to register/unregister sensors and models ---------------- */

    private void registerSensor(Sensor s, AccumulatorsFactory factory, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        unregisterSensor(s, listeners);

        Preference<Integer> sensorsMinDelayMillis = mPreferences.sensorsReadingsDelayMillis();
        Accumulator<?> accum = factory.newSensor(s, acc -> {
                    acc.setMinDelayMillis(sensorsMinDelayMillis.get());
                    acc.consumers().add((sensorEvent, row) -> {
                        row.put("label", mLabel);
                    });
                    Consumer<Preference<Integer>> cbk = p -> acc.setMinDelayMillis(p.get());
                    listeners.put(s, cbk);
                    sensorsMinDelayMillis.registerListener(cbk);
        });
        mAccumulators.put(s, accum);
        logger().i("Registering sensor (%s)", s.getName());
    }


    private void unregisterSensor(Sensor s, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        Preference<Integer> sensorsMinDelayMillis = mPreferences.sensorsReadingsDelayMillis();

        if (listeners.containsKey(s))
            sensorsMinDelayMillis.unregisterListener(listeners.remove(s));
        if (mAccumulators.containsKey(s))
            logger().i("Unregistering sensor (%s)", s.getName());
        mAccumulators.remove(s);
    }


    private void registerModel(AccumulatorTFModel model, AccumulatorsFactory factory, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        unregisterModel(model, listeners);

        Preference<Integer> modelsMinDelayMillis = mPreferences.modelsReadingsDelayMillis();
        Accumulator<?> accum = factory.newTFModel(model, (acc) -> {
            acc.setMinDelayMillis(modelsMinDelayMillis.get());
            acc.consumers().add((event, row) -> {
                String activity = mLabel;
                row.put("label", activity);
            });
            Consumer<Preference<Integer>> cbk = p -> acc.setMinDelayMillis(p.get());
            listeners.put(model, cbk);
            modelsMinDelayMillis.registerListener(cbk);
        });
        mAccumulators.put(model, accum);
        mAccumulators.getLifecycle().addObserver(model.getLifecycleDelegate());
        logger().i("Registering model (%s)", model.getName());
    }

    private void unregisterModel(AccumulatorTFModel model, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        Preference<Integer> modelsMinDelayMillis = mPreferences.modelsReadingsDelayMillis();
        if (listeners.containsKey(model))
            modelsMinDelayMillis.unregisterListener(listeners.remove(model));
        if (mAccumulators.containsKey(model))
            logger().i("Unregistering model (%s)", model.getName());
        mAccumulators.getLifecycle().removeObserver(model.getLifecycleDelegate());
        mAccumulators.remove(model);
    }


    /* ----------- END Helper functions to register/unregister sensors and models ---------------- */



    /**
     * Save the currently gathered sensor readings through saveZipClearSensorsFiles().
     * @param intent the calling intent. The extra EXTRA_ZIP_PREFIX can be set to select the zip prefix
     */
    private void saveRecording(Intent intent)
    {
        if (!isRecording())
            return;
        saveZipClearSensorsFiles(null);

        // Notify user through vibration
        AndroidUtils.getVibrator(this).vibrate(
                VibrationEffect.createOneShot(
                        100,
                        VibrationEffect.DEFAULT_AMPLITUDE)
        );

    }



    /**
     * Sets the label for the sensor readings. The classification
     * will be appended to the sensor dataframes
     * @param intent the intent requesting the change of classification. Should contain the extra
     *               EXTRA_SENSOR_CLASSIFICATION
     */
    private void setLabel(Intent intent)
    {
        if(!isRecording())
            return;
        if (intent.hasExtra(EXTRA_SENSOR_LABEL))
        {
            mLabel = intent.getStringExtra(EXTRA_SENSOR_LABEL);
            logger().i("Setting sensors label to: %s", mLabel);
        }
        else
            mLabel = null;
    }


    private void startRecurrentSave(Intent intent)
    {
        stopRecurrentSave();

        long saveIntervalMillis = TimeUnit.MILLISECONDS.convert(
                Math.max(1, mPreferences.saveIntervalMinutes().get()),
                TimeUnit.MINUTES
        );

        if (saveIntervalMillis > 0)
        {
            mRepeatingBroadcast.start(saveIntervalMillis, ((context, intent1) -> {
                Intent saveIntent = new Intent(this, RecordService.class);
                saveIntent.setAction(ACTION_SAVE_ZIP_CLEAR);
                startService(saveIntent);
            }));
            logger().i("Recurrently saving every %s seconds", TimeUnit.SECONDS.convert(saveIntervalMillis, TimeUnit.MILLISECONDS));
        }
    }

    private void stopRecurrentSave()
    {
        if (mRepeatingBroadcast.isBroadcasting())
            logger().i("Stopping to recurrently save");
        mRepeatingBroadcast.stop();
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
    private Callable<Integer[]> saveZipClearSensorsFiles(@Nullable String zipPrefix)
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
     * Delete the folder where the sensor readings have been saved
     * The method returns a Callable because Persistence uses AsyncTasks to perform file operations.
     * @return A Callable to access the result of the operation.
     * !! The operation performs even if the call() method is not called !!
     */
    private Callable<Integer> deleteSaveFolder()
    {
        return Persistence.SENSORS_FOLDER.deleteSaveFolder();
    }



    /**
     * Register a BroadcastReceiver that will listen for events sent through broadcasts.
     * Broadcasts must set action ACTION_SEND_EVENT and extras
     * EXTRA_ACCUMULATOR_ID (String), EXTRA_EVENT (RowParcelable)
     * Sending and receiving event through broadcast is heavily affected by performance issues. It is
     * therefore advised to utilize this mechanism only for gathering rare events
     * @param accumulatorId the accumulatorId to use
     * @return whether the operation was successful
     */
    public boolean setupBroadcastReceiver(String accumulatorId)
    {
        if (mBroadcastReceivers.containsKey(accumulatorId))
            return false;
        Accumulator<DataFrame.Row> accumulator = AccumulatorsFactory
                .newInstance(this)
                .newRowAccumulator(accumulatorId, null);

        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (!isRecording() || !intent.hasExtra(EXTRA_ACCUMULATOR_ID) || !intent.hasExtra(EXTRA_EVENT) ||
                        !intent.getStringExtra(EXTRA_ACCUMULATOR_ID).equals(accumulatorId))
                    return;
                RowParcelable event = intent.getParcelableExtra(EXTRA_EVENT);
                accumulator.accept(event.getRow());
            }
        };
        mAccumulators.put(accumulatorId, accumulator);
        mBroadcastReceivers.put(accumulatorId, receiver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RECORD_EVENT);
        registerReceiver(receiver, filter);
        return true;
    }

    /**
     * Removes a broadcast receiver and its corresponding accumulator
     * @param accumulatorId id of the accumulator to remove
     * @return whether the operation was successful
     */
    public boolean removeBroadcastReceiver(String accumulatorId)
    {
        if (!mBroadcastReceivers.containsKey(accumulatorId))
            return false;
        BroadcastReceiver receiver = mBroadcastReceivers.remove(accumulatorId);
        mAccumulators.remove(accumulatorId);
        unregisterReceiver(Objects.requireNonNull(receiver));
        return true;
    }

    /**
     * Clear all broadcast receivers
     * @return number of broadcast receivers removed
     */
    private int clearBroadcastReceivers()
    {
        int num = mBroadcastReceivers.size();
        for(String sender : Lists.newArrayList(mBroadcastReceivers.keySet()))
            removeBroadcastReceiver(sender);
        return num;
    }

}