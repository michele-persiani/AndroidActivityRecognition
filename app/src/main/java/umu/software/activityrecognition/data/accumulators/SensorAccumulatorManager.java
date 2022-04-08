package umu.software.activityrecognition.data.accumulators;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import umu.software.activityrecognition.common.lifecycles.LifecycleElement;
import umu.software.activityrecognition.common.Factory;
import umu.software.activityrecognition.common.AndroidUtils;


public class SensorAccumulatorManager implements LifecycleElement
{
    private final Factory<SensorAccumulator> mFactory;
    private final int mDelay;
    private final int[] mSensorTypes;
    private SensorManager mSensorManager;

    private Map<Integer, Sensor> mSensors = new HashMap<>();
    private Map<Integer, SensorAccumulator> mListeners = new HashMap<>();

    private final boolean mPrivateHandler;
    private Handler mHandler;

    /**
     *
     * @param factory Factory that will be used to instantiate all of the accumulators used by the manager
     * @param delay The Android Sensor.DELAY_X identifier to use when registering for sensors
     */
    public SensorAccumulatorManager(Factory<SensorAccumulator> factory, boolean privateHandler, int delay, int... sensorTypes)
    {
        this.mDelay = delay;
        this.mFactory = factory;
        this.mSensorTypes = sensorTypes;
        mPrivateHandler = privateHandler;
    }



    /**
     * Initialize the manager by fetching the available sensors from the system
     * @param context the Android context. Usually an Activity or Service
     */
    public void onCreate(Context context)
    {
        mHandler = mPrivateHandler? AndroidUtils.newHandler() : AndroidUtils.newMainLooperHandler();
        mSensorManager = AndroidUtils.getSensorManager(context);
        List<Sensor> sensors = Arrays.stream(mSensorTypes)
                .mapToObj(type -> mSensorManager.getDefaultSensor(type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        mSensors.clear();
        for (int i = 0; i < sensors.size(); i++)
            mSensors.put(i, sensors.get(i));
        Log.i(getClass().getSimpleName(), "onCreate()");
        mListeners.clear();
    }

    @Override
    public void onStart(Context context)
    {
        startRecordings();
    }

    @Override
    public void onStop(Context context)
    {
        stopRecordings();
    }


    public void onDestroy(Context context)
    {
        onStop(context);
        if(mPrivateHandler)
            mHandler.getLooper().quitSafely(); // Quit the handler that receives the sensor events
    }

    /**
     *
     * @return All sensors
     */
    public HashMap<Integer, Sensor> getSensors()
    {
        return new HashMap<>(mSensors);
    }

    /**
     *
     * @return All registered accumulators
     */
    public HashMap<Integer, SensorAccumulator> getAccumulators()
    {
        return new HashMap<>(mListeners);
    }



    /**
     * Get the sensor with the given id
     * @param sensorId the id of the sensor to retrieve.
     * @return the Android Sensor, or null if no sensor with that id
     */
    public Sensor getSensor(int sensorId)
    {
        return mSensors.get(sensorId);
    }

    /**
     * Get the accumulator with the given id
     * @param sensorId the id of the accumulator to retrieve.
     * @return the accumulator, or null if no accumulator with the given sensor is present
     */
    public SensorAccumulator getAccumulator(int sensorId)
    {
        return mListeners.get(sensorId);
    }


    /**
     * Iterate all the accumulators
     * @param consumer the BiConsumer that
     */
    public void forEachAccumulator(BiConsumer<Integer, SensorAccumulator> consumer)
    {
        for (Map.Entry<Integer, SensorAccumulator> acc : getAccumulators().entrySet())
            consumer.accept(acc.getKey(), acc.getValue());
    }



    /**
     * Register a SensorAccumulator to the sensor with the given Id. The Id order is determined
     * by the order of the sensor returned by SensorManager
     * @param sensorId the id of the SensorAccumulator to start
     * @return whether the operation was successful
     */
    public boolean startRecording(int sensorId)
    {
        if (mSensorManager == null || sensorId < 0 || sensorId > mSensors.size())
            return false;
        else if (!mSensors.containsKey(sensorId))
            return false;
        else if (mListeners.containsKey(sensorId))
            return true;
        else if (getSensor(sensorId).getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT)
        {
            Log.e(getClass().getSimpleName(), "Sensors with REPORTING_MODE_ONE_SHOT are not supported.");
            return false;
        }
        else
        {
            Sensor sensor = getSensor(sensorId);
            Log.i(getClass().getSimpleName(), String.format("Registering %s...", sensor.getName()));
            SensorAccumulator accum = mFactory.make();
            mListeners.put(sensorId, accum);
            boolean result = mSensorManager.registerListener(accum, sensor, mDelay, mHandler);
            Log.i(getClass().getSimpleName(), "...done");
            return result;
        }
    }

    /**
     * Start recording from all accumulators
     * @return the number of running accumulators
     */
    public int startRecordings()
    {
        Set<Integer> keys = mSensors.keySet();
        int startedSensors = 0;
        for (Integer e : keys)
        {
            boolean res = startRecording(e);
            startedSensors += res ? 1 : 0;
        }
        return startedSensors;
    }

    /**
     * Stop recording from the accumulator with given id
     * @param sensorId the id of the accumulator to stop
     * @return whether an accumulator was recording and got stopped
     */
    public boolean stopRecording(int sensorId)
    {
        if (mSensorManager == null)
            return false;
        if (mListeners.containsKey(sensorId))
        {
            Sensor sensor = mSensors.get(sensorId);
            SensorEventListener listener = mListeners.get(sensorId);
            Log.i(getClass().getSimpleName(), String.format("Unregistering %s...", sensor.getName()));
            mSensorManager.unregisterListener(listener, sensor);
            Log.i(getClass().getSimpleName(), "...done");
            return true;
        }
        return false;
    }

    /**
     * Stop all accumulators
     * @return the number of stopped accumulators
     */
    public int stopRecordings()
    {
        Set<Map.Entry<Integer, SensorAccumulator>> keys = new HashSet<>(mListeners.entrySet());
        int stoppedSensors = 0;
        for (Map.Entry<Integer, SensorAccumulator> e : keys)
        {
            boolean res = stopRecording(e.getKey());
            stoppedSensors += res ? 1 : 0;
        }
        return stoppedSensors;
    }

    /**
     * Resets all accumulators
     * @return number of reset accumulators
     */
    public int resetAccumulators()
    {
        for (SensorAccumulator acc : mListeners.values())
            acc.clearDataFrame();
        return mListeners.size();
    }
}
