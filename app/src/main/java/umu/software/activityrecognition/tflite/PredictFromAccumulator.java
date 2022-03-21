package umu.software.activityrecognition.tflite;

import com.c_bata.DataFrame;
import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.common.Factory;
import umu.software.activityrecognition.sensors.accumulators.Accumulators;
import umu.software.activityrecognition.sensors.accumulators.SensorAccumulator;

public class PredictFromAccumulator extends PredictTemplate
{

    Map<Integer, SensorAccumulator> accumulators;            // Map (tensor_num, accumulator)
    Map<Integer, String> sensorMappings;                     // Map (tensor_num, sensor_name)
    Map<Integer, String[]> inputMappings;                    // Map (tensor_num, sensor_columns)

    public PredictFromAccumulator(Interpreter interpreter)
    {
        super(interpreter);
        accumulators = Maps.newHashMap();
        sensorMappings = Maps.newHashMap();
        inputMappings = Maps.newHashMap();
    }

    @Override
    protected boolean isReady()
    {
        if (accumulators.size() < interpreter.getInputTensorCount())
            return false;

        for (Integer tensorNum : accumulators.keySet())
        {
            if (accumulators.get(tensorNum).countReadings() < inputSequenceLength(tensorNum))
                return false;
        }
        return true;
    }

    @Override
    protected void writeInputBuffer(int tensorNum, FloatBuffer buffer)
    {

        DataFrame df = getDataFrame(tensorNum);

        final int[] added = {0};
        df.forEachRow(objects -> {
            if (added[0] >= inputSequenceLength(tensorNum))
                return null;
            for (Object o : objects) {
                buffer.put(Float.parseFloat(o.toString()));
            }
            added[0] += 1;
            return null;
        });


    }

    public SensorAccumulator setAccumulator(int tensorNum, String sensorName, String[] columns)
    {
        Factory<SensorAccumulator> factory = Accumulators.newSlideWindowFactory(
                inputSequenceLength(tensorNum)
        );
        SensorAccumulator accum = factory.make();
        accumulators.put(tensorNum, accum);
        sensorMappings.put(tensorNum, sensorName);
        inputMappings.put(tensorNum, columns);
        return accum;
    }

    public void forEachAccumulator(BiConsumer<? super Integer, ? super SensorAccumulator> fun)
    {
        accumulators.forEach(fun);
    }


    protected DataFrame getDataFrame(int tensorNum)
    {
        assert accumulators.containsKey(tensorNum);
        assert inputMappings.containsKey(tensorNum);

        DataFrame accumDataframe = accumulators.get(tensorNum).getDataFrame();
        String[] selectedColumns = inputMappings.get(tensorNum);
        DataFrame df             = new DataFrame();

        for (String col : selectedColumns) {
            assert accumDataframe.containsKey(col);
            df.put(col, accumDataframe.get(col));
        }

        return df;
    }
}
