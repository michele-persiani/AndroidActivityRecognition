package umu.software.activityrecognition.tflite;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import umu.software.activityrecognition.common.lifecycles.LifecycleElement;
import umu.software.activityrecognition.common.AndroidUtils;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;


public enum SensorTensorflowLiteModels implements LifecycleElement
{
    SOM("som.tflite",
            InputTensorDefinition
                    .with("Accelerometer", 50, "f_0", "f_1", "f_2", "delta_timestamp")
                    .and("Gyroscope", 50, "f_0", "f_1", "f_2", "delta_timestamp")
    );



    private final String byteModelFilePath;
    private Handler handler;

    private boolean initialized = false;
    private AccumulatorTFModel model;
    private final InputTensorDefinition tensorDefinition;


    SensorTensorflowLiteModels(String byteModelFilePath, InputTensorDefinition tensorsDefinition)
    {
        this.byteModelFilePath = byteModelFilePath;
        this.tensorDefinition = tensorsDefinition;
    }

    @Override
    public void onCreate(Context context)
    {
        if (initialized)
            return;
        MappedByteBuffer byteModel = loadByteModel(context, byteModelFilePath);
        if (byteModel == null)
            return;

        Interpreter interpreter = new Interpreter(byteModel);
        interpreter.allocateTensors();
        model = new AccumulatorTFModel(interpreter);

        handler = AndroidUtils.newHandler();

        // Initialize accumulators
        tensorDefinition.forEachAccumulator((i, acc) -> {
            model.setAccumulator(i, acc);
            acc.setWindowSize(model.inputSequenceLength(i));
        });

        initialized = true;
    }

    @Override
    public void onStart(Context context)
    {
        if(!initialized)
            return;

        // Register accumulators
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);

        tensorDefinition.forEachSensorAccumulator((sensorName, accumulator) -> {
            Sensor sensor = findSensor(sensorManager, sensorName);
            assert sensor != null;
            sensorManager.registerListener(accumulator, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler);
        });
    }

    @Override
    public void onStop(Context context)
    {
        if(!initialized)
            return;

        // Unregister accumulators
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        tensorDefinition.forEachSensorAccumulator((sensorName, accumulator) -> {
            sensorManager.unregisterListener(accumulator);
        });
    }

    @Override
    public void onDestroy(Context context)
    {
        onStop(context);
        handler.getLooper().quitSafely();
    }



    public boolean predict()
    {
        return model.predict();
    }


    public float[][] getOutput(int outputNum)
    {
        return model.getOutput(outputNum);
    }



    private static Sensor findSensor(SensorManager sensorManager, String name)
    {
        for(Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            if (s.getName() == name)
                return s;
        return null;
    }


    private static MappedByteBuffer loadByteModel(Context context, String modelFilename)
    {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer byteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            return byteModel;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e(SensorTensorflowLiteModels.class.getSimpleName(), "! Exception while load tensorflow model !");
            return null;
        }
    }

}
