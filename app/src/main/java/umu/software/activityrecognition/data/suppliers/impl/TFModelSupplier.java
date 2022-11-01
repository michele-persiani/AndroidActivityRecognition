package umu.software.activityrecognition.data.suppliers.impl;

import java.nio.FloatBuffer;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.tflite.model.TFModel;

/**
 * Supplier that reads from a TFModel
 */
public class TFModelSupplier implements DataSupplier
{

    private final TFModel model;

    public TFModelSupplier(TFModel model)
    {
        this.model = model;
    }



    @Override
    public void initialize()
    {

    }

    @Override
    public boolean isReady()
    {
        return model.predict();
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public String getName()
    {
        return model.getName();
    }


    @Override
    public void accept(DataFrame.Row row)
    {
        if(!model.predict())
            return;

        for (int i = 0; i < model.getOutputTensorCount(); i++)
        {
            int seqLen = model.getOutputSequenceLength(i);
            int outSize = model.getOutputSize(i);
            float[][] res = new float[seqLen][outSize];

            FloatBuffer buffer = model.getOutput(i);
            buffer.rewind();
            for (int j = 0; j < res.length; j++)
                for (int k = 0; k < res[j].length; k++)
                    row.put(String.format("%s_%s_%s", i, j, k), buffer.get());
        }
    }
}
