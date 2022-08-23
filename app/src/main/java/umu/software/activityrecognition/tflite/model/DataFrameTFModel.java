package umu.software.activityrecognition.tflite.model;

import umu.software.activityrecognition.data.dataframe.DataFrame;

import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.nio.FloatBuffer;
import java.util.Map;

/**
 * Model that feeds its tensors using dataframes
 */
public abstract class DataFrameTFModel extends TFModel
{
    private final Map<Integer, DataFrame> inputDataframes = Maps.newHashMap();

    /**
     * @param name name of the model
     * @param interpreter Tensorflow Lite Interpreter
     */
    public DataFrameTFModel(String name, Interpreter interpreter)
    {
        super(name, interpreter);
    }

    /**
     * Check whether getTensorData() returns data that is suitable to feed the tensors.
     *  If getTensorData() returns a dataframe with too few rows, or with a wrong number of columns,
     *  will return false, true otherwise.
     *  Whenever isInputReady() return false the current computation is skipped
     * @return whether all input dataframes are ready
     */
    @Override
    protected boolean isInputReady()
    {
        for (int i = 0; i < interpreter.getInputTensorCount(); i++)
            inputDataframes.put(i, new DataFrame());

        DataFrame df;
        for (Integer tensorNum : inputDataframes.keySet())
        {
            df = getInputDataFrame(tensorNum);
            if (df == null || df.size() != getInputSize(tensorNum) || (df.countRows() < getInputSequenceLength(tensorNum)))
                return false;
            inputDataframes.put(tensorNum, df);
        }
        return true;
    }

    @Override
    protected void writeInputBuffer(int tensorNum, FloatBuffer buffer)
    {
        DataFrame df = inputDataframes.get(tensorNum);
        assert df != null;

        while(df.countRows() > getInputSequenceLength(tensorNum))
            df.popFirstRow();

        df.forEachRowArray(objects -> {
            for (Object o : objects)
                buffer.put(Float.parseFloat(o.toString()));
            return null;
        });
    }


    /**
     * Get all output dataframes
     * @return mapping between output tensors id and dataframes, or null if the model is not ready
     * eg. by not having enough input
     */
    public Map<Integer, DataFrame> getOutputDataFrames()
    {
        boolean success = predict();
        if (!success) return null;
        Map<Integer, DataFrame> result = Maps.newHashMap();
        for (int i = 0; i < getOutputTensorCount(); i++)
            result.put(i, getOutputDataFrame(i));
        return result;
    }


    /**
     * Getter for the dataframe for the given tensor number. The dataframe contains the currently avaialabe
     * data for the tensor. If insufficient in number of rows, isInputReady() will return false.
     * @param tensorNum the tensor of the dataset
     * @return the dataframe or null if there is an error
     */
    protected abstract DataFrame getInputDataFrame(int tensorNum);


    public DataFrame getOutputDataFrame(int i)
    {
        int seqLen = getOutputSequenceLength(i);
        int outSize = getOutputSize(i);

        FloatBuffer buffer = getOutput(i);
        buffer.rewind();
        DataFrame df = new DataFrame(String.format("%s:%s", getName(), i));
        for (int j = 0; j < seqLen; j++)
        {
            DataFrame.Row row = new DataFrame.Row();
            for (int k = 0; k < outSize; k++)
                row.put(String.format("f_%s", k), buffer.get());
            df.appendRow(row);
        }
        return df;
    }
}
