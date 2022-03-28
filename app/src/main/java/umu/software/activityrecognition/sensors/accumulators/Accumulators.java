package umu.software.activityrecognition.sensors.accumulators;
import java.util.function.Consumer;

import umu.software.activityrecognition.common.Factory;

/**
 * Factory methods for SensorAccumulator and SensorAccumulatorManager objects
 */
public class Accumulators
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
     * Get a new SlideWindowSensorAccumulator factory
     * @param windowSize size of the accumulators' window
     * @param initializer Initializer for the newly created accumulators. Useful to add consumers to the accumulator.
     * @return the accumulators factory
     */
    public static Factory<SensorAccumulator> newSlideWindowFactory(int windowSize, Consumer<SensorAccumulator> initializer)
    {
        return () -> {
            SensorAccumulator acc = new SlideWindowSensorAccumulator(windowSize);
            initializer.accept(acc);
            return acc;
        };
    }


    public static Factory<SensorAccumulator> newSlideWindowFactory(int windowSize)
    {
        return () -> new SlideWindowSensorAccumulator(windowSize);
    }

    /**
     *
     * @param factory the factory used to create sensor accumulators
     * @param delay a SensorManager delay constant, such as SensorManager.SENSOR_DELAY_NORMAL
     * @return
     */
    public static SensorAccumulatorManager newAccumulatorManager(Factory<SensorAccumulator> factory, int delay)
    {
        return new SensorAccumulatorManager(factory, delay);
    }


    public static SensorAccumulatorManager newAccumulatorManager(Factory<SensorAccumulator> factory)
    {
        return new SensorAccumulatorManager(factory);
    }

}
