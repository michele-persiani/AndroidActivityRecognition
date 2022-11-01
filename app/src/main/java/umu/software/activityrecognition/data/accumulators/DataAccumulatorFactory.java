package umu.software.activityrecognition.data.accumulators;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;

import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataPipe;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.data.suppliers.impl.SensorEventSupplier;
import umu.software.activityrecognition.data.suppliers.impl.TFLiteAudioClassifierSupplier;
import umu.software.activityrecognition.data.suppliers.impl.TFModelSupplier;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.tflite.model.DataTFModel;
import umu.software.activityrecognition.tflite.model.TFModel;


/**
 * Factory for Accumulators
 */
public class DataAccumulatorFactory
{
    private final SensorManager sensorManager;
    private final Context context;


    protected DataAccumulatorFactory(Context context, SensorManager sensorManager)
    {
        this.context = context.getApplicationContext();
        this.sensorManager = sensorManager;
    }


    /**
     * Creates a new instance of the factory
     * @param context the calling context
     * @return a new factory instance
     */
    public static DataAccumulatorFactory newInstance(Context context)
    {
        return new DataAccumulatorFactory(context, AndroidUtils.getSensorManager(context));
    }

    /**
     * Creates a new accumulator getting data from a fixed row (nb. row values can be dynamically changed)
     * @return a newly created accumulator
     */
    public DataAccumulator newRowValues(String name, DataFrame.Row values, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        DataSupplier supp = new DataSupplier()
        {
            @Override
            public String getName(){return name;}

            @Override
            public void initialize(){}

            @Override
            public boolean isReady(){ return true; }

            @Override
            public void dispose(){}

            @Override
            public void accept(DataFrame.Row row){row.putAll(values);}
        };
        return make(supp, initializer);
    }


    /**
     * Create a SensorAccumulator for the default sensor of the given sensorType
     * @param sensorType the sensor type of the sensor to add.
     * @param initializer optional builder to initialize the SensorAccumulator
     * @return a newly created accumulator
     */
    public DataAccumulator newDefaultSensor(int sensorType, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        return newSensor(sensor, initializer);
    }


    /**
     * Create a SensorAccumulator for the given sensor
     * @param sensor the sensor to get the readings from
     * @param initializer optional builder to initialize the SensorAccumulator
     * @return a newly created accumulator
     */
    public DataAccumulator newSensor(Sensor sensor, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        SensorEventSupplier supp = new SensorEventSupplier(sensorManager, sensor);
        return make(supp, initializer);
    }



    /**
     * Create an accumulator for the given TFModel
     * @param model the TFModel to use
     * @param initializer optional builder to initialize the TFModelAccumulator
     * @return a newly created accumulator
     */
    public DataAccumulator newTFModel(TFModel model, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        TFModelSupplier supp = new TFModelSupplier(model);
        return make(supp, initializer);
    }



    /**
     * Create an accumulator for the given TFModel
     * @param model the DataTFModel to use.
     * @param initializer optional initializer
     * @return a newly created accumulator
     */
    public DataAccumulator newTFModel(DataTFModel model, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        TFModelSupplier supp = new TFModelSupplier(model)
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
        };
        return make(supp, initializer);
    }

    /**
     * Create an accumulator for data coming from the TFLite audio classifier
     * @param modelAssetsFileName name of the tflite model file inside the assets folder
     * @param initializer optional initializer
     * @return a newly created accumulator
     */
    public DataAccumulator newAudioClassifier(String modelAssetsFileName, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        DataSupplier supp = new TFLiteAudioClassifierSupplier(context, modelAssetsFileName);
        return make(supp, initializer);
    }





    private DataAccumulator make(DataSupplier supplier, @Nullable Consumer<DataPipe.Builder> initializer)
    {
        DataPipe.Builder builder = DataPipe.startWith(supplier);

        if (initializer != null)
            initializer.accept(builder);
        DataSupplier pipe = builder.build();
        DataAccumulator accum = new DataAccumulator();
        accum.setSupplier(pipe);
        return accum;
    }
}
