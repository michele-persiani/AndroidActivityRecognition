package umu.software.activityrecognition.data.accumulators;


import umu.software.activityrecognition.data.accumulators.consumers.EventConsumersFactory;
import umu.software.activityrecognition.data.dataframe.DataFrame;

import java.nio.FloatBuffer;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import umu.software.activityrecognition.tflite.model.TFModel;

/**
 * Accumulators recording a Tensorflow Lite model. Each row of the dataframe will be a prediction from the model
 */
public class TFModelAccumulator extends Accumulator<TFModel>
{
    private final Supplier<TFModel> modelSupplier;


    public TFModelAccumulator(TFModel model)
    {
        this.modelSupplier = () -> model;
    }


    public TFModelAccumulator(Supplier<TFModel> provider)
    {
        this.modelSupplier = provider;
    }

    @Override
    protected boolean filter(TFModel event)
    {
        return super.filter(event) && event.predict();
    }


    @Override
    protected void initializeConsumers(Queue<BiConsumer<TFModel, DataFrame.Row>> eventConsumers)
    {
        eventConsumers.add((template, row) -> {
            for (int i = 0; i < template.getOutputTensorCount(); i++)
            {
                int seqLen = template.getOutputSequenceLength(i);
                int outSize = template.getOutputSize(i);
                float[][] res = new float[seqLen][outSize];

                FloatBuffer buffer = template.getOutput(i);
                buffer.rewind();
                for (int j = 0; j < res.length; j++)
                    for (int k = 0; k < res[j].length; k++)
                        row.put(String.format("%s_%s_%s", i, j, k), buffer.get());

            }
        });
        eventConsumers.add(EventConsumersFactory.newSystemClockTimestamp());
        eventConsumers.add(EventConsumersFactory.newEpochTimestamp());
    }


    @Override
    public void startRecording()
    {
        super.startRecording();
        startSupplier(modelSupplier);
    }

    @Override
    public void stopRecording()
    {
        super.stopRecording();
        stopSupplier();
    }


    @Override
    protected String getDataFrameName()
    {
        return modelSupplier.get().getName();
    }
}
