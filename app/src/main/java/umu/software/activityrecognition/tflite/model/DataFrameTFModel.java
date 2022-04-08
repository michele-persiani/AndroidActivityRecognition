package umu.software.activityrecognition.tflite.model;

import com.c_bata.DataFrame;
import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class DataFrameTFModel extends TFModel
{
    private final Map<Integer, DataFrame> dataframes = Maps.newHashMap();

    /**
     * @param interpreter a Tensorflow Lite Interpreter
     */
    public DataFrameTFModel(Interpreter interpreter)
    {
        super(interpreter);
        for (int i = 0; i < interpreter.getInputTensorCount(); i++)
            dataframes.put(i, new DataFrame());
    }


    @Override
    protected boolean isInputReady()
    {
        DataFrame df;

        if (dataframes.size() < interpreter.getInputTensorCount())
            return false;

        for (Integer tensorNum : dataframes.keySet())
        {
            df = getDataFrame(tensorNum);
            if (df == null || df.size() != inputSize(tensorNum) || (df.countRows() < inputSequenceLength(tensorNum)))
                return false;
            dataframes.put(tensorNum, df);
        }
        return true;
    }

    @Override
    protected void writeInputBuffer(int tensorNum, FloatBuffer buffer)
    {
        DataFrame df = dataframes.get(tensorNum);
        assert df != null;
        double elapsedTime = df.get("delta_timestamp").stream().mapToDouble(a -> Double.parseDouble(a.toString())).sum();

        //Log.i("PredictTemplate", "Time window: "+elapsedTime);
        //int dfSize = df.size() * df.countRows();
        //if (dfSize != buffer.capacity())
        //    Log.e("", "dfSize != buffer.capacity()");

        AtomicInteger addedRows = new AtomicInteger(0);
        df.forEachRow(objects -> {
            if (addedRows.get() >= inputSequenceLength(tensorNum))
                return null;

            for (Object o : objects)
                buffer.put(Float.parseFloat(o.toString()));
            addedRows.addAndGet(1);

            return null;
        });
    }

    /**
     * Getter for the dataframe for the given tensor number
     * @param tensorNum the tensor of the dataset
     * @return the dataframe or null if there is an error
     */
    protected abstract DataFrame getDataFrame(int tensorNum);
}
