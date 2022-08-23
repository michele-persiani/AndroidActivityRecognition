package umu.software.activityrecognition.tflite;
import android.content.Context;
import android.hardware.Sensor;

import java.util.List;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.AccumulatorsFactory;
import umu.software.activityrecognition.data.accumulators.consumers.EventConsumersFactory;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;


/**
 * Tensorflow lite named models
 */
public enum TFLiteNamedModels
{
    SOM("som.tflite", (context, accumulators) -> {
        long minDelayMillis = 50;

        double[] accelMeanValues = new double[]{-2.0740206, -6.1852093,  4.6671915};
        double[] accelStdValues = new double[]{3.619758,  2.9208457, 3.6106684};

        double[] gyroMeanValues = new double[]{0.00014184, -0.0038549,   0.00325081};
        double[] gyroStdValues = new double[]{0.5485127,  0.34025237, 0.40774605};

        double[] gravMeanValues = new double[]{-2.1514955, -6.134693,   4.633932};
        double[] gravStdValues = new double[]{3.5410628, 2.7907739, 3.4992359};

        AccumulatorsFactory factory = AccumulatorsFactory.newInstance(context);
        accumulators.add(
                factory.newDefaultSensor(Sensor.TYPE_ACCELEROMETER, (accum) -> {
                    accum.setMinDelayMillis(minDelayMillis);
                    accum.consumers().add(EventConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"));
                    accum.consumers().add(EventConsumersFactory.newSubValues("f_", accelMeanValues));
                    accum.consumers().add(EventConsumersFactory.newDivideByValues("f_", accelStdValues));
                })
        );

        accumulators.add(
                factory.newDefaultSensor(Sensor.TYPE_GYROSCOPE, (accum) -> {
                    accum.setMinDelayMillis(minDelayMillis);
                    accum.consumers().add(EventConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"));
                    accum.consumers().add(EventConsumersFactory.newSubValues("f_", gyroMeanValues));
                    accum.consumers().add(EventConsumersFactory.newDivideByValues("f_", gyroStdValues));
                })
        );

        accumulators.add(
                factory.newDefaultSensor(Sensor.TYPE_GRAVITY, (accum) -> {
                    accum.setMinDelayMillis(minDelayMillis);
                    accum.consumers().add(EventConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"));
                    accum.consumers().add(EventConsumersFactory.newSubValues("f_", gravMeanValues));
                    accum.consumers().add(EventConsumersFactory.newDivideByValues("f_", gravStdValues));
                })
        );
    });


    private final BiConsumer<Context, List<Accumulator<?>>> builder;
    private final String byteModelFilePath;

    AccumulatorTFModel mModel;

    TFLiteNamedModels(String byteModelFilePath, BiConsumer<Context, List<Accumulator<?>>> accumulatorManager)
    {
        this.byteModelFilePath = byteModelFilePath;
        this.builder = accumulatorManager;
    }

    public String getModelName()
    {
        return byteModelFilePath;
    }

    public AccumulatorTFModel newInstance(Context context)
    {
        TFLiteFactory factory = TFLiteFactory.newInstance(context);
        return factory.newAccumulatorAssetModel(
                getModelName(),
                byteModelFilePath,
                accumulators -> builder.accept(context, accumulators)
                );
    }


}
