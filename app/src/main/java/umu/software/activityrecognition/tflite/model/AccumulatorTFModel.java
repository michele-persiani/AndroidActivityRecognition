package umu.software.activityrecognition.tflite.model;

import com.c_bata.DataFrame;
import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.util.Map;

import umu.software.activityrecognition.data.accumulators.Accumulator;


/**
 * Implementation of the predict template that uses the accumulators
 */
public class AccumulatorTFModel extends DataFrameTFModel
{

    Map<Integer, Accumulator<?>> accumulators;            // Map (tensor_num, accumulator)


    public AccumulatorTFModel(Interpreter interpreter)
    {
        super(interpreter);
        accumulators = Maps.newHashMap();
    }


    public void setAccumulator(int tensorNum, Accumulator<?> accumulator)
    {
        accumulators.put(tensorNum, accumulator);
    }


    protected DataFrame getDataFrame(int tensorNum)
    {
        assert accumulators.containsKey(tensorNum);
        return accumulators.get(tensorNum).getDataFrame();
    }
}
