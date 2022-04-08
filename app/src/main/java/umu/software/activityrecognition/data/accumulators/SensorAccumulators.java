package umu.software.activityrecognition.data.accumulators;
import android.hardware.SensorManager;

import java.util.function.Consumer;

import umu.software.activityrecognition.common.Factory;

/**
 * Factory methods for SensorAccumulator and SensorAccumulatorManager objects
 */
public class SensorAccumulators
{

    /**
     * Get a new SensorAccumulator factory
     * @param initializer Initializer for the newly created accumulators. Useful to add consumers to the accumulator.
     * @return the accumulators factory
     */
    public static Factory<SensorAccumulator> newFactory(Consumer<SensorAccumulator> initializer)
    {
        return () -> {
            SensorAccumulator acc = new SensorAccumulator();
            initializer.accept(acc);
            return acc;
        };
    }


    public static Factory<SensorAccumulator> newFactory()
    {
        return () -> new SensorAccumulator();
    }


    /**
     * Create a new SensorAccumulatorManager
     * @param factory the factory used to create sensor accumulators
     * @param privateHandler whether to run the accumulators on a private thread
     * @param sensorTypes list of sensor types to register to. For each sensor type the manager will register
     *                    to the default sensor
     * @return
     */

    public static SensorAccumulatorManager newAccumulatorManager(Factory<SensorAccumulator> factory, boolean privateHandler, int... sensorTypes)
    {
        return new SensorAccumulatorManager(factory, privateHandler, SensorManager.SENSOR_DELAY_NORMAL, sensorTypes);
    }

}
