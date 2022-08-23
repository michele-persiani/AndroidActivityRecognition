package umu.software.activityrecognition.tflite.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.AccumulatorsLifecycle;
import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.shared.lifecycles.LifecycleDelegateObserver;

import org.tensorflow.lite.Interpreter;

import java.util.List;


/**
 * Implementation of the TFModel template that utilizes an AccumulatorsLifecycle to get the input dataframes.
 * This model has a lifecycle that is used to start/stop the accumulators
 */
public class AccumulatorTFModel extends DataFrameTFModel implements LifecycleOwner
{
    private final AccumulatorsLifecycle accumulatorManager;
    private final LifecycleDelegateObserver observer;

    /**
     * @param name name of the model
     * @param interpreter the TFLite interpreter encapsulating the binary tflite model
     * @param accumulators list of accumulators to use. Must be the same size of the input tensors
     */
    public AccumulatorTFModel(String name, Interpreter interpreter, List<Accumulator<?>> accumulators)
    {
        super(name, interpreter);
        assert accumulators.size() == getInputTensorCount();
        accumulatorManager = new AccumulatorsLifecycle();
        observer = new LifecycleDelegateObserver(getLifecycle());

        Accumulator<?> accum;
        for (int i = 0; i < getInputTensorCount(); i++)
        {
            accum = accumulators.get(i);
            accumulatorManager.put(i, accum);
            accum.setWindowSize(getInputSequenceLength(i));
        }
    }


    protected DataFrame getInputDataFrame(int tensorNum)
    {
        assert accumulatorManager.containsKey(tensorNum);
        return accumulatorManager.getDataFrame(tensorNum);
    }

    /**
     * Returns the accumulators' lifecycle. Can be used, for example, to start/stop the accumulators
     * @return the LifecycleRegistry handling the accumulators' lifecycle
     */
    @NonNull
    @Override
    public LifecycleRegistry getLifecycle()
    {
        return accumulatorManager.getLifecycle();
    }

    /**
     * Returns an observer that allows to hook the accumulators lifecycles to another
     * @return observer that allows to hook the accumulators lifecycles to another
     */
    public LifecycleObserver getLifecycleDelegate()
    {
        return observer;
    }

}
