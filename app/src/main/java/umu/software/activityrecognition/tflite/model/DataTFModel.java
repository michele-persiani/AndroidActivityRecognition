package umu.software.activityrecognition.tflite.model;

import android.util.Pair;

import com.google.common.collect.Lists;

import org.tensorflow.lite.Interpreter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.accumulators.DataAccumulator;


public class DataTFModel extends DataFrameTFModel
{
    private final List<DataAccumulator> accumulators = Lists.newArrayList();


    public DataTFModel(String name, Interpreter interpreter, DataAccumulator... inputs)
    {
        this(name, interpreter, Lists.newArrayList(inputs));
    }


    public DataTFModel(String name, Interpreter interpreter, List<DataAccumulator> inputs)
    {
        super(name, interpreter);
        assert inputs.size() == getInputTensorCount();

        AtomicInteger id = new AtomicInteger(0);
        inputs.forEach(acc -> {
            int i = id.getAndIncrement();
            acc.setWindowSize(getInputSequenceLength(i));
            accumulators.add(acc);
        });
    }

    /**
     * Executes a command on each input
     * @param cmd command to execute
     */
    public void forEachInput(BiConsumer<Integer,DataAccumulator> cmd)
    {
        AtomicInteger i = new AtomicInteger(0);
        accumulators.stream().map(acc -> Pair.create(i.getAndIncrement(), acc)).forEach(p -> cmd.accept(p.first, p.second));
    }

    /**
     * Starts receiving input data
     */
    public void startDataSupply()
    {
        forEachInput((i, acc) -> acc.startRecording());
    }

    /**
     * Stops the input data.
     */
    public void stopDataSupply()
    {
        forEachInput((i, acc) -> acc.stopRecording());
    }


    @Override
    protected DataFrame getInputDataFrame(int tensorNum)
    {
        return accumulators.get(tensorNum).getDataFrame();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        stopDataSupply();
    }
}
