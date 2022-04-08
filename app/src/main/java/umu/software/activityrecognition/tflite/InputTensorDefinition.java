package umu.software.activityrecognition.tflite;



import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.data.accumulators.Accumulator;
import umu.software.activityrecognition.data.accumulators.SensorAccumulator;


public class InputTensorDefinition
{
    Map<String, SensorAccumulator> sensorAccumulators = Maps.newHashMap();
    List<Accumulator<?>> accumulators = Lists.newArrayList();


    public void forEachAccumulator(BiConsumer<Integer, Accumulator<?>> consumer)
    {
        for (int i = 0; i < accumulators.size(); i++)
            consumer.accept(i, accumulators.get(i));
    }


    public void forEachSensorAccumulator(BiConsumer<String, SensorAccumulator> consumer)
    {
        for (Map.Entry<String, SensorAccumulator> e : sensorAccumulators.entrySet())
            consumer.accept(e.getKey(), e.getValue());
    }


    public static InputTensorDefinition with(String sensorName, long accumMinDelayMillis, String... columns)
    {
        InputTensorDefinition definition = new InputTensorDefinition();
        return definition.and(sensorName, accumMinDelayMillis, columns);
    }


    public InputTensorDefinition and(String sensorName, long accumMinDelayMillis, String... columns)
    {
        SensorAccumulator accumulator = new SensorAccumulator();
        accumulator.setMinDelayMillis(accumMinDelayMillis);

        accumulator.consumers().add((event, row) -> {
            if (columns.length == 0)
                return;
            for (String col : Lists.newArrayList(row.keySet()))
                if (!Arrays.asList(columns).contains(col))
                    row.remove(col);
        });
        sensorAccumulators.put(sensorName, accumulator);
        accumulators.add(accumulator);
        return this;
    }


}
