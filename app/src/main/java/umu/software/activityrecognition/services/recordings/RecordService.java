package umu.software.activityrecognition.services.recordings;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;

import android.hardware.Sensor;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleObserver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.data.accumulators.AccumulatorsMap;
import umu.software.activityrecognition.data.accumulators.DataAccumulator;
import umu.software.activityrecognition.data.accumulators.DataAccumulatorFactory;
import umu.software.activityrecognition.data.suppliers.DataPipe;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.data.persistence.DataFrameWriterFactory;
import umu.software.activityrecognition.preferences.RecordServicePreferences;
import umu.software.activityrecognition.shared.lifecycles.ExclusiveResourceLifecycle;
import umu.software.activityrecognition.shared.persistance.Directories;
import umu.software.activityrecognition.shared.resourceaccess.ExclusiveResource;
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
import umu.software.activityrecognition.tflite.TFLiteNamedModels;


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
 *  Allows to register and manage external accumulators through setupBroadcastReceiver(),
 *  removeBroadcastReceiver() and clearBroadcastReceivers()
 *  The service also allows binding to directly access its methods
 */
public class RecordService extends LifecycleService
{
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



    private LifecycleObserver mWakeLockLifecycle;
    private ForegroundServiceLifecycle mFregroundLifecycle;
    private AccumulatorsMap mAccumulators;
    private RepeatingBroadcast mSaveRepeatingBroadcast;
    private RecordServicePreferences mPreferences;
    private ServiceBinder<RecordService> mBinder;
    private boolean mRecording = false;
    private String mLabel = null;
    private ExclusiveResourceLifecycle mTokensLifecycle;


    @Override
    public IBinder onBind(Intent intent)
    {
        if (mBinder == null)
            mBinder = new ServiceBinder<>(this);
        return mBinder;
    }


    @SuppressLint("LaunchActivityFromNotification")
    @Override
    public void onCreate()
    {
        super.onCreate();
        mRecording = false;
        mSaveRepeatingBroadcast = new RepeatingBroadcast(this);
        mAccumulators = new AccumulatorsMap();
        getLifecycle().addObserver(new LifecycleDelegateObserver(mAccumulators.getLifecycle()));
        mPreferences = new RecordServicePreferences(this);

        mWakeLockLifecycle = WakeLockLifecycle.newPartialWakeLock(this);


        /* Exclusive resource access */
        mTokensLifecycle = new ExclusiveResourceLifecycle();
        getLifecycle().addObserver(mTokensLifecycle);

        mTokensLifecycle.registerToken(TFLiteNamedModels.AUDIO_CLASSIFIER,
                ExclusiveResource.PRIORITY_LOW,
                ExclusiveResource.AUDIO_INPUT
        );


        /* Foreground notification */

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
                            .setVibrate(new long[]{ 0 })
                            .setGroup(getString(R.string.notification_group_id));
                }
        );


        registerAction(this::onStartRecording, ACTION_START_RECORDING);
        registerAction(this::onStopRecording, ACTION_STOP_RECORDING);
        registerAction(this::onStartRecurrentSave, ACTION_START_RECURRENT_SAVE);
        registerAction(this::onStopRecurrentSave, ACTION_STOP_RECURRENT_SAVE);
        registerAction(this::onSaveRecording, ACTION_SAVE_ZIP_CLEAR);
        registerAction(this::onSetLabel, ACTION_SET_CLASSIFICATION);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        if (!isRecording())
            stopSelf();
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        onStopRecurrentSave(null);
        stopForeground(true);
        stopSelf();
        mAccumulators.clear();
        mPreferences.clearListeners();
        logger().i("Service destroyed");
    }


    /**
     * Returns whether the service is currently recording
     * @return whether the service is currently recording
     */
    public boolean isRecording()
    {
        return mRecording;
    }



    private void onStopRecording(Intent intent)
    {
        mRecording = false;
    }


    /**
     * Start recording sensor data
     * @param intent the intent requesting to start recording.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         If intent is null we use the default values
     */
    private void onStartRecording(@Nullable Intent intent)
    {
        if(isRecording())
            return;

        mRecording = true;
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


        DataAccumulatorFactory factory = DataAccumulatorFactory.newInstance(this);

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
            if (recordModel.test(namedModel))
            {
                registerModel(namedModel, preferenceListeners);
                modelNames.add(namedModel.getModelName());
            }

            mPreferences.recordModel(namedModel.getModelName()).registerListener( p -> {
                if (p.get())
                    registerModel(namedModel, preferenceListeners);
                else
                    unregisterModel(namedModel, preferenceListeners);
            });

        }




        if (useWakeLock.get())
            getLifecycle().addObserver(mWakeLockLifecycle);
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

    private void registerSensor(Sensor s, DataAccumulatorFactory factory, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        Preference<Integer> sensorsMinDelayMillis = mPreferences.sensorsReadingsDelayMillis();


        unregisterSensor(s, listeners);

        DataAccumulator acc = factory.newSensor(s, pipe -> {
            pipe.then(row -> row.put("label", mLabel));
        });
        acc.setDelayMillis(sensorsMinDelayMillis.get());

        Consumer<Preference<Integer>> cbk = p -> acc.setDelayMillis(p.get());
        listeners.put(s, cbk);
        sensorsMinDelayMillis.registerListener(cbk);

        mAccumulators.put(s, acc);
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


    private void registerModel(TFLiteNamedModels model, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        logger().i("Registering model (%s)", model.getModelName());
        Preference<Integer> modelsMinDelayMillis = mPreferences.modelsReadingsDelayMillis();

        unregisterModel(model, listeners);

        DataSupplier supp = DataPipe
                .startWith(model.newDataSupplier(this))
                .then(row -> row.put("label", mLabel))
                .build();
        DataAccumulator accum = new DataAccumulator(supp);
        accum.setDelayMillis(modelsMinDelayMillis.get());

        Consumer<Preference<Integer>> cbk = p -> accum.setDelayMillis(p.get());
        listeners.put(model, cbk);
        modelsMinDelayMillis.registerListener(cbk);

        mAccumulators.put(model, accum);

        if (mTokensLifecycle.hasToken(model))
        {
            mTokensLifecycle.getToken(model).setResourceAcquiredCallback(accum::startRecording);
            mTokensLifecycle.getToken(model).setResourceReleasedCallback(accum::stopRecording);
            mTokensLifecycle.getToken(model).acquire(false);
        }
    }


    private void unregisterModel(TFLiteNamedModels model, Map<Object, Consumer<Preference<Integer>>> listeners)
    {
        Preference<Integer> modelsMinDelayMillis = mPreferences.modelsReadingsDelayMillis();
        if (listeners.containsKey(model))
            modelsMinDelayMillis.unregisterListener(listeners.remove(model));
        if (mAccumulators.containsKey(model))
            logger().i("Unregistering model (%s)", model.getModelName());
        mAccumulators.remove(model);


        if (mTokensLifecycle.hasToken(model))
        {
            mTokensLifecycle.getToken(model).setResourceAcquiredCallback(null);
            mTokensLifecycle.getToken(model).setResourceReleasedCallback(null);
            mTokensLifecycle.getToken(model).release();
        }
    }


    /* ----------- END Helper functions to register/unregister sensors and models ---------------- */



    /**
     * Perform saving, compression, and clearing files in succession.
     * @param intent calling intent
     */
    private void onSaveRecording(Intent intent)
    {
        if (!isRecording())
            return;

        String saveDirectory = mPreferences.saveFolderPath().get();
        List<DataFrame> dataframes = getDataFrames();
        List<String> fileNames = dataframes.stream().map(df -> df.getName() + ".csv").collect(Collectors.toList());


        clearDataFrames();
        runAsync(() -> {

            boolean result = Directories.peformOnDirectory(
                    saveDirectory,
                    null,
                    dir -> {
                        Function<DataFrame, String> dataframeWriter = DataFrameWriterFactory.newToCSV(true, ",");

                        dir.delete(fileNames::contains);

                        for (int i = 0; i < dataframes.size(); i++)
                        {
                            int j = i;
                            dir.writeToFile(
                                    fileNames.get(i),
                                    os -> {
                                        OutputStreamWriter osw = new OutputStreamWriter(os);
                                        String dfString = dataframeWriter.apply(dataframes.get(j));
                                        osw.write(dfString);
                                        osw.close();
                                        return null;
                                    });
                        }

                        String zipName = String.format("%s.zip", dir.listFileNames(fn -> !fn.startsWith(".") && fn.endsWith(".zip")).size());
                        Directories.createZip(dir, zipName, fileNames);
                        dir.delete(fileNames::contains);
                        return null;
                    });

            logger().i("Dataframe save: %s", (result)? "success" : "FAILURE");

        });
    }



    /**
     * Sets the label for the sensor readings. The classification
     * will be appended to the sensor dataframes
     * @param intent the intent requesting the change of classification. Should contain the extra
     *               EXTRA_SENSOR_CLASSIFICATION
     */
    private void onSetLabel(Intent intent)
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


    private void onStartRecurrentSave(@Nullable Intent intent)
    {
        onStopRecurrentSave(intent);

        long saveIntervalMillis = TimeUnit.MILLISECONDS.convert(
                Math.max(1, mPreferences.saveIntervalMinutes().get()),
                TimeUnit.MINUTES
        );

        if (saveIntervalMillis > 0)
        {
            mSaveRepeatingBroadcast.start(saveIntervalMillis, ((context, intent1) -> {
                Intent saveIntent = new Intent(this, RecordService.class);
                saveIntent.setAction(ACTION_SAVE_ZIP_CLEAR);
                startService(saveIntent);
            }));
            logger().i("Recurrently saving every %s seconds", TimeUnit.SECONDS.convert(saveIntervalMillis, TimeUnit.MILLISECONDS));
        }

    }


    private void onStopRecurrentSave(Intent intent)
    {
        if (mSaveRepeatingBroadcast.isBroadcasting())
            logger().i("Stopping to recurrently save");
        mSaveRepeatingBroadcast.stop();
    }


    /**
     * Fetches dataframes from the accumulator manager
     * @return collection of dataframes
     */
    private List<DataFrame> getDataFrames()
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



}