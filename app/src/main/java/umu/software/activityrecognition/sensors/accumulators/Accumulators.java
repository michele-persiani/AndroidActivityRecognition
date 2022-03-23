package umu.software.activityrecognition.sensors.accumulators;

import android.hardware.SensorManager;

import umu.software.activityrecognition.common.Factory;

/**
 * Factory methods for SensorAccumulator and SensorAccumulatorManager objects
 */
public class Accumulators
{

    public static Factory<SensorAccumulator> newFactory()
    {
        return () -> new SensorAccumulator();
    }


    public static Factory<SensorAccumulator> newSlideWindowFactory(int windowSize)
    {
        return () -> new SlideWindowSensorAccumulator(windowSize);
    }


    public static SensorAccumulatorManager newAccumulatorManager(Factory<SensorAccumulator> factory, int delay)
    {
        return new SensorAccumulatorManager(factory, delay);
    }


    public static SensorAccumulatorManager newAccumulatorManager(Factory<SensorAccumulator> factory)
    {
        return newAccumulatorManager(factory, SensorManager.SENSOR_DELAY_NORMAL);
    }

}
