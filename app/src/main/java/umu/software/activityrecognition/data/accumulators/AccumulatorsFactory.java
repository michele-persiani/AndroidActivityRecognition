package umu.software.activityrecognition.data.accumulators;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.AccelerometerCompassProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.CalibratedGyroscopeProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor1Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor2Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.RotationVectorProvider;

import java.util.function.Consumer;

import umu.software.activityrecognition.shared.AndroidUtils;
import umu.software.activityrecognition.tflite.model.TFModel;


/**
 * Factory for Accumulators
 */
public class AccumulatorsFactory
{
    private final SensorManager sensorManager;


    protected AccumulatorsFactory(SensorManager sensorManager)
    {
        this.sensorManager = sensorManager;
    }


    /**
     * Creates a new instance of the factory
     * @param context the calling context
     * @return a new factory instance
     */
    public static AccumulatorsFactory newInstance(Context context)
    {
        return new AccumulatorsFactory(AndroidUtils.getSensorManager(context));
    }



    /**
     * Creates a new instance of the factory
     * @param sensorManager the sensorManager that will be used by the accumulators
     * @return a new factory instance
     */
    public static AccumulatorsFactory newInstance(SensorManager sensorManager)
    {
        return new AccumulatorsFactory(sensorManager);
    }


    /**
     * Create and add a SensorAccumulator for the default sensor of the given sensorType
     * @param sensorType the sensor type of the sensor to add.
     * @param initializer builder to initialize the SensorAccumulator
     */
    public Accumulator<SensorEvent> newDefaultSensor(int sensorType, Consumer<Accumulator<SensorEvent>> initializer)
    {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        return newSensor(sensor, initializer);
    }


    /**
     * Create a SensorAccumulator for the given sensor
     * @param sensor the sensor to get the readings from
     * @param initializer builder to initialize the SensorAccumulator
     */
    public Accumulator<SensorEvent> newSensor(Sensor sensor, Consumer<Accumulator<SensorEvent>> initializer)
    {
        SensorAccumulator accum = new SensorAccumulator(sensorManager, sensor);
        initializer.accept(accum);
        return accum;
    }


    /**
     * Create an accumulator for the given TFModel
     * @param model a supplier for the TFModel to use
     * @param initializer builder to initialize the TFModelAccumulator
     */
    public Accumulator<TFModel> newTFModel(TFModel model, Consumer<Accumulator<TFModel>> initializer)
    {
        TFModelAccumulator accum = new TFModelAccumulator(model);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newProvider1Accumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor1Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newProvider2Accumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newGravityCompassAccumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newAccelerometerCompassAccumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new AccelerometerCompassProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newRotationVectorAccumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new RotationVectorProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newCalibratedGyroscopeAccumulator(Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new CalibratedGyroscopeProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }
}
