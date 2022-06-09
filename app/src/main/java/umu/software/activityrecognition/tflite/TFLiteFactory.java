package umu.software.activityrecognition.tflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import com.google.api.client.util.Lists;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.function.Consumer;

import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.tflite.model.AccumulatorTFModel;


public class TFLiteFactory
{
    private final Context mContext;


    public TFLiteFactory(Context context)
    {
        mContext = context;
    }


    /**
     * Creates a TFLite model loading its binary file from the assets folder
     * @param modelAssetBinaryFile name of the binary file in the assets folder
     * @param inputsBuilder builder for the input accumulators
     * @return
     */
    public AccumulatorTFModel newAccumulatorAssetModel(String modelAssetBinaryFile, Consumer<List<Accumulator<?>>> inputsBuilder)
    {
        Interpreter interpreter = loadInterpreterFromAssets(mContext, modelAssetBinaryFile);
        if (interpreter == null)
            return null;
        List<Accumulator<?>> inputs = Lists.newArrayList();
        inputsBuilder.accept(inputs);

        return new AccumulatorTFModel(modelAssetBinaryFile, interpreter, inputs);
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
