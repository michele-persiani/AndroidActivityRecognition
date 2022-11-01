package umu.software.activityrecognition.tflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import umu.software.activityrecognition.data.accumulators.DataAccumulator;
import umu.software.activityrecognition.tflite.model.DataTFModel;


/**
 * Factory to create Tensorflow Lite models
 */
public class TFLiteFactory
{
    private final Context mContext;


    private TFLiteFactory(Context context)
    {
        mContext = context;
    }


    /**
     * Creates a TFLite model loading its binary file from the assets folder
     * @param name name of the model
     * @param modelAssetBinaryFile name of the binary file in the assets folder. eg. 'classifier.tflite'
     *                             the file is to be put in the /assets folder
     * @param inputs input accumulators
     * @return a new AccumulatorTFModel
     */
    public DataTFModel newAccumulatorAssetModel(String name, String modelAssetBinaryFile, DataAccumulator... inputs)
    {
        Interpreter interpreter = loadInterpreterFromAssets(mContext, modelAssetBinaryFile);
        if (interpreter == null)
            throw new RuntimeException("Error loading the assets file");
        return new DataTFModel(name, interpreter, inputs);
    }


    /**
     * Creates a TFLite model loading its binary file from the assets folder
     * @param name name of the model
     * @param interpreter TFLite Interpreter encapsulating a binary model
     * @param inputs input accumulators
     * @return a new AccumulatorTFModel
     */
    public DataTFModel newAccumulatorModel(String name, Interpreter interpreter, DataAccumulator... inputs)
    {

        return new DataTFModel(name, interpreter, inputs);
    }



    public static TFLiteFactory newInstance(Context context)
    {
        return new TFLiteFactory(context.getApplicationContext());
    }



    /**
     * Load an interpreter from a binary file in the assets folder
     * @param context the calling context
     * @param modelFilename the filename of the binary model
     * @return the TFLite Interpreter
     */
    public static Interpreter loadInterpreterFromAssets(Context context, String modelFilename)
    {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer byteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            Interpreter interpreter = new Interpreter(byteModel);
            interpreter.allocateTensors();
            return interpreter;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e("Exception", "! Exception while load tensorflow model !");
            return null;
        }
    }
}
