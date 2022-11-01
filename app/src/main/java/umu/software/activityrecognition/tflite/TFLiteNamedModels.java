package umu.software.activityrecognition.tflite;

import android.content.Context;
import android.hardware.Sensor;

import java.util.function.BiFunction;

import umu.software.activityrecognition.data.accumulators.DataAccumulator;
import umu.software.activityrecognition.data.accumulators.DataAccumulatorFactory;
import umu.software.activityrecognition.data.consumers.DataConsumersFactory;
import umu.software.activityrecognition.data.suppliers.DataPipe;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.data.suppliers.impl.TFLiteAudioClassifierSupplier;
import umu.software.activityrecognition.data.suppliers.impl.TFModelSupplier;
import umu.software.activityrecognition.tflite.model.DataTFModel;




/**
 * Tensorflow lite named models
 */
public enum TFLiteNamedModels
{
    SOM(
            "som.tflite",
            (context, modelAssetsname) -> {
        long minDelayMillis = 50;

        double[] accelMeanValues = new double[]{-2.0740206, -6.1852093, 4.6671915};
        double[] accelStdValues = new double[]{3.619758, 2.9208457, 3.6106684};

        double[] gyroMeanValues = new double[]{0.00014184, -0.0038549, 0.00325081};
        double[] gyroStdValues = new double[]{0.5485127, 0.34025237, 0.40774605};

        double[] gravMeanValues = new double[]{-2.1514955, -6.134693, 4.633932};
        double[] gravStdValues = new double[]{3.5410628, 2.7907739, 3.4992359};


        DataAccumulatorFactory accumFctry = DataAccumulatorFactory.newInstance(context);
        DataTFModel model = new DataTFModel(
                modelAssetsname,
                TFLiteFactory.loadInterpreterFromAssets(context, modelAssetsname),
                accumFctry.newDefaultSensor(Sensor.TYPE_ACCELEROMETER, builder ->
                        {
                            builder.then(DataConsumersFactory.newEpochTimestamp("sensor"))
                                    .then(DataConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"))
                                    .then(DataConsumersFactory.newSubValues("f_", accelMeanValues))
                                    .then(DataConsumersFactory.newDivideByValues("f_", accelStdValues));
                        }
                ),
                accumFctry.newDefaultSensor(Sensor.TYPE_GYROSCOPE, builder ->
                        {
                            builder.then(DataConsumersFactory.newEpochTimestamp("sensor"))
                                    .then(DataConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"))
                                    .then(DataConsumersFactory.newSubValues("f_", gyroMeanValues))
                                    .then(DataConsumersFactory.newDivideByValues("f_", gyroStdValues));
                        }
                ),
                accumFctry.newDefaultSensor(Sensor.TYPE_GRAVITY, builder ->
                        {
                            builder.then(DataConsumersFactory.newEpochTimestamp("sensor"))
                                    .then(DataConsumersFactory.newSelectColumns("f_0", "f_1", "f_2", "sensor_delta_timestamp"))
                                    .then(DataConsumersFactory.newSubValues("f_", gravMeanValues))
                                    .then(DataConsumersFactory.newDivideByValues("f_", gravStdValues));
                        }
                )
        );
        model.forEachInput((i, acc) -> acc.setDelayMillis(minDelayMillis));

        return DataPipe.startWith(new TFModelSupplier(model)
                {
                    @Override
                    public void initialize()
                    {
                        super.initialize();
                        model.startDataSupply();
                    }

                    @Override
                    public void dispose()
                    {
                        super.dispose();
                        model.stopDataSupply();
                    }
                })
                .then(DataConsumersFactory.newEpochTimestamp("som_model"))
                .build();
    }),
    AUDIO_CLASSIFIER(
            "yamnet.tflite",
            TFLiteAudioClassifierSupplier::new);



    private final BiFunction<Context, String, DataSupplier> builder;
    private final String byteModelFilePath;



    TFLiteNamedModels(String byteModelFilePath, BiFunction<Context, String, DataSupplier> accum)
    {
        this.byteModelFilePath = byteModelFilePath;
        this.builder = accum;
    }


    public String getModelName()
    {
        return byteModelFilePath;
    }



    public DataSupplier newDataSupplier(Context context)
    {
        return builder.apply(context, byteModelFilePath);
    }


    public DataAccumulator newAccumulator(Context context)
    {
        return new DataAccumulator(builder.apply(context, byteModelFilePath));
    }
}
