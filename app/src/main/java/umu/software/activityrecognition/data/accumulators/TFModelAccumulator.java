package umu.software.activityrecognition.data.accumulators;


import com.c_bata.DataFrame;

import java.util.Queue;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.tflite.model.TFModel;

/**
 * Accumulators accepting a Tensorflow Lite model. Each row of the dataframe will be a prediction from the model
 */
public class TFModelAccumulator extends Accumulator<TFModel>
{

    @Override
    protected boolean filter(TFModel event)
    {
        return super.filter(event) && event.predict();
    }


    @Override
    protected void initializeConsumers(Queue<BiConsumer<TFModel, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((template, row) -> {
            for (int i = 0; i < template.getInputTensorCount(); i++)
            {
                float[][] result = template.getOutput(i);
                for (int j = 0; j < result.length; j++)
                    for (int k = 0; k < result[k].length; k++)
                        row.put(String.format("%s_%s_%s", i, j, k), result[i][j]);
            }
        });
    }


}
