package umu.software.activityrecognition.tflite.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.AccumulatorsLifecycle;
import umu.software.activityrecognition.data.dataframe.DataFrame;

import org.tensorflow.lite.Interpreter;

import java.util.List;


/**
 * Implementation of the TFModel template that utilizes an AccumulatorManager to get the input dataframes.
 * The accumulator manager is to be externally initialized, and the keys utilized to get the dataframes
 * are the incremental tensor indices 0-(N-1), where N is the number of input tensors.
 */
public class AccumulatorTFModel extends DataFrameTFModel implements LifecycleOwner
{
    private final AccumulatorsLifecycle accumulatorManager;


    public AccumulatorTFModel(String name, Interpreter interpreter, List<Accumulator<?>> accumulators)
    {
        super(name, interpreter);
        accumulatorManager = new AccumulatorsLifecycle();

        assert accumulators.size() == getInputTensorCount();
        for (int i = 0; i < getInputTensorCount(); i++)
            accumulatorManager.put(i, accumulators.get(i));
    }


    protected DataFrame getInputDataFrame(int tensorNum)
    {
        assert accumulatorManager.containsKey(tensorNum);
        return accumulatorManager.getDataFrame(tensorNum);
    }

    @NonNull
    @Override
    public LifecycleRegistry getLifecycle()
    {
        return accumulatorManager.getLifecycle();
    }
}
