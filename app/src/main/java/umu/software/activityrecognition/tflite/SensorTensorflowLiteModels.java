package umu.software.activityrecognition.tflite;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;

import androidx.core.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import umu.software.activityrecognition.common.lifecycles.LifecycleElement;
import umu.software.activityrecognition.common.AndroidUtils;


public enum SensorTensorflowLiteModels implements LifecycleElement
{

    ENCODER_GRAVITY("encoder.tflite",
            SensorManager.SENSOR_DELAY_NORMAL,
            InputTensorsDefinition
                    .with("gravity  Non-wakeup", "f_0", "f_1", "f_2", "delta_timestamp")
    );


    private static class InputTensorsDefinition extends ArrayList<Pair<String, String[]>>
    {

        public static InputTensorsDefinition with(String sensorName, String... columns)
        {
            InputTensorsDefinition t = new InputTensorsDefinition();
            t.add(Pair.create(sensorName, columns));
            return t;
        }

        public InputTensorsDefinition then(String sensorName, String... columns)
        {
            add(Pair.create(sensorName, columns));
            return this;
        }
    }

    // Mapping -> sensor_name, input_tensor_num, window_size

    private final String filePath;
    private final int samplingDelay;
    private Handler handler;

    private boolean initialized = false;
    private PredictFromAccumulator template;
    private final InputTensorsDefinition tensorDefinition;


    SensorTensorflowLiteModels(String filePath, int samplingDelay, InputTensorsDefinition tensorsDefinition)
    {
        this.filePath = filePath;
        this.samplingDelay = samplingDelay;
        this.tensorDefinition = tensorsDefinition;
    };

    @Override
    public void onCreate(Context context)
    {
        if (initialized)
            return;
        MappedByteBuffer model;
        try {
            model = loadModelFile(context, filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Interpreter interpreter = new Interpreter(model);
        interpreter.allocateTensors();
        template = new PredictFromAccumulator(interpreter);

        handler = AndroidUtils.newHandler("tflite-"+filePath);
        initializeAccumulators();
        initialized = true;
    }

    @Override
    public void onStart(Context context)
    {
        if(initialized)
            registerAccumulators(context);
    }

    @Override
    public void onStop(Context context)
    {
        unregisterAccumulators(context);
    }

    @Override
    public void onDestroy(Context context)
    {
        onStop(context);
        handler.getLooper().quitSafely();
    }

    public boolean predict()
    {
        return template.predict();
    }


    public float[][] getOutput(int outputNum)
    {
        return template.getOutput(outputNum);
    }


    private void initializeAccumulators()
    {
        String[] columns;
        for (int i = 0; i < tensorDefinition.size(); i++)
        {
            columns = tensorDefinition.get(i).second;
            template.setAccumulator(i, columns);
        }

    }

    private void registerAccumulators(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        template.forEachAccumulator((inputNum, accum) -> {
            Pair<String, String[]> sensorColumns = tensorDefinition.get(inputNum);
            String  sensorName = sensorColumns.first;
            Sensor sensor = findSensor(sensorManager, sensorName);
            assert sensor != null;
            sensorManager.registerListener(accum, sensor, samplingDelay, handler);
        });
    }

    private void unregisterAccumulators(Context context)
    {
        SensorManager sensorManager = AndroidUtils.getSensorManager(context);
        template.forEachAccumulator((inputNum, accum) -> {
            sensorManager.unregisterListener(accum);
        });
    }

    private static Sensor findSensor(SensorManager sensorManager, String name)
    {
        for(Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL))
            if (s.getName() == name)
                return s;
        return null;
    }



    private static MappedByteBuffer loadModelFile(Context context, String modelFilename) throws IOException
    {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
