package umu.software.activityrecognition.data.accumulators;

import android.content.Context;
import android.hardware.SensorManager;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.AccelerometerCompassProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.CalibratedGyroscopeProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor1Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor2Provider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.RotationVectorProvider;
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion;

import java.util.Queue;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.data.accumulators.consumers.EventConsumersFactory;
import umu.software.activityrecognition.data.dataframe.DataFrame;

/**
 * Accumulator for OrientationProvider objects from https://github.com/apacha/sensor-fusion-demo
 *
 * The constructor is private, use the factory methods to instantiate objects
 */
public class OrientationProviderAccumulator extends Accumulator<OrientationProvider>
{
    private final OrientationProvider orientationProvider;


    public OrientationProviderAccumulator(OrientationProvider orientationProvider)
    {
        this.orientationProvider = orientationProvider;
    }

    @Override
    protected void initializeConsumers(Queue<BiConsumer<OrientationProvider, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((evt, row) -> {
            float[] eulerAngles = new float[3];
            evt.getEulerAngles(eulerAngles);
            row.put("euler_0", eulerAngles[0]);
            row.put("euler_1", eulerAngles[1]);
            row.put("euler_2", eulerAngles[2]);
        });
        eventConsumers.add((evt, row) -> {
            Quaternion q = new Quaternion();
            evt.getQuaternion(q);
            row.put("quaternion_x", q.getX());
            row.put("quaternion_y", q.getY());
            row.put("quaternion_z", q.getZ());
            row.put("quaternion_w", q.getW());
        });
        eventConsumers.add(EventConsumersFactory.newEpochTimestamp());
    }




    @Override
    protected String getDataFrameName()
    {
        return OrientationProvider.class.getSimpleName();
    }


    @Override
    public void startRecording()
    {
        super.startRecording();
        orientationProvider.start();
        startSupplier(() -> orientationProvider);
    }

    @Override
    public void stopRecording()
    {
        super.stopRecording();
        stopSupplier();
        orientationProvider.stop();
    }


    /* Factory methods */

    public static OrientationProviderAccumulator makeProvider1Accumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new ImprovedOrientationSensor1Provider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }

    public static OrientationProviderAccumulator makeProvider2Accumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }

    public static OrientationProviderAccumulator makeGravityCompassAccumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }

    public static OrientationProviderAccumulator makeAccelerometerCompassAccumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new AccelerometerCompassProvider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }

    public static OrientationProviderAccumulator makeRotationVectorAccumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new RotationVectorProvider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }

    public static OrientationProviderAccumulator makeCalibratedGyroscopeAccumulator(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        OrientationProvider orientationProvider = new CalibratedGyroscopeProvider(sensorManager);
        return new OrientationProviderAccumulator(orientationProvider);
    }
}
