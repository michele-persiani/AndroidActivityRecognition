package umu.software.activityrecognition.data.accumulators;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.AccelerometerCompassProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.CalibratedGyroscopeProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor1Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor2Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.RotationVectorProvider;

import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.shared.util.AndroidUtils;
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
     * Creates a new accumulator that allows to manually add rows
     * @param dataframeName the name of the accumulator's dataframe
     * @return the accumulator
     */
    public Accumulator<DataFrame.Row> newRowAccumulator(String dataframeName, @Nullable Consumer<Accumulator<DataFrame.Row>> initializer)
    {
        RowAccumulator accum = new RowAccumulator(dataframeName);
        if (initializer != null)
            initializer.accept(accum);
        return accum;
    }

    /**
     * Create a SensorAccumulator for the default sensor of the given sensorType
     * @param sensorType the sensor type of the sensor to add.
     * @param initializer optional builder to initialize the SensorAccumulator
     */
    public Accumulator<SensorEvent> newDefaultSensor(int sensorType, @Nullable Consumer<Accumulator<SensorEvent>> initializer)
    {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        return newSensor(sensor, initializer);
    }


    /**
     * Create a SensorAccumulator for the given sensor
     * @param sensor the sensor to get the readings from
     * @param initializer optional builder to initialize the SensorAccumulator
     */
    public Accumulator<SensorEvent> newSensor(Sensor sensor, @Nullable Consumer<Accumulator<SensorEvent>> initializer)
    {
        SensorAccumulator accum = new SensorAccumulator(sensorManager, sensor);
        if (initializer != null)
            initializer.accept(accum);
        return accum;
    }


    /**
     * Create an accumulator for the given TFModel
     * @param model a supplier for the TFModel to use
     * @param initializer optional builder to initialize the TFModelAccumulator
     */
    public Accumulator<TFModel> newTFModel(TFModel model, @Nullable Consumer<Accumulator<TFModel>> initializer)
    {
        TFModelAccumulator accum = new TFModelAccumulator(model);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newProvider1Accumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor1Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newProvider2Accumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newGravityCompassAccumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newAccelerometerCompassAccumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new AccelerometerCompassProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newRotationVectorAccumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new RotationVectorProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }


    public Accumulator<OrientationProvider> newCalibratedGyroscopeAccumulator(@Nullable Consumer<Accumulator<OrientationProvider>> initializer)
    {
        OrientationProvider orientationProvider = new CalibratedGyroscopeProvider(sensorManager);
        Accumulator<OrientationProvider> accum = new OrientationProviderAccumulator(orientationProvider);
        if(initializer != null)
            initializer.accept(accum);
        return accum;
    }
}
