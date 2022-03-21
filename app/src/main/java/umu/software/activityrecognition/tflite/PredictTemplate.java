package umu.software.activityrecognition.tflite;


import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class PredictTemplate
{
    protected final Interpreter interpreter;
    private final ReentrantLock lock;

    private FloatBuffer[] inputs;
    private HashMap<Integer, FloatBuffer> outputs;


    public PredictTemplate(Interpreter interpreter)
    {
        this.interpreter = interpreter;
        lock = new ReentrantLock();
        makeBuffers();

    }

    private void makeBuffers()
    {
        int inputsCount = interpreter.getInputTensorCount();
        int outputsCount = interpreter.getOutputTensorCount();

        inputs = new FloatBuffer[inputsCount];

        for (int i = 0; i < inputsCount; i++)
            inputs[i] = makeInputFloatBuffer(i);

        outputs = new HashMap<>();

        for (int i = 0; i < outputsCount; i++)
            outputs.put(i, makeOutputFloatBuffer(i));
    }


    public boolean predict()
    {
        int inputsCount = interpreter.getInputTensorCount();
        int outputsCount = interpreter.getOutputTensorCount();

        if (!isReady())
            return false;

        FloatBuffer buffer;

        for (int i = 0; i < inputsCount; i++)
        {
            buffer  = inputs[i];
            buffer.rewind();
            writeInputBuffer(i, buffer);
            buffer.rewind();
        }

        Map<Integer, Object> outMap = Maps.newHashMap();
        lock.lock();
        for (int i = 0; i < outputsCount; i++)
        {
            buffer  = outputs.get(i);
            buffer.rewind();
            outMap.put(i, buffer);
        }

        interpreter.runForMultipleInputsOutputs(inputs, outMap);
        lock.unlock();
        return true;
    }



    public float[][] getOutput(int outputNum)
    {
       assert 0 >= outputNum && outputNum < interpreter.getOutputTensorCount();

        lock.lock();
        FloatBuffer buffer = outputs.get(outputNum).duplicate();
        lock.unlock();
        buffer.rewind();
        int seqLen = outputSequenceLength(outputNum);
        int outSize = outputSize(outputNum);
        float[][] res = new float[seqLen][outSize];

        for (int i=0; i < seqLen;i++)
            for (int j = 0; j < outSize; j++)
                res[i][j] = buffer.get();


        return res;
    }


    public int inputSize(int tensorNum)
    {
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        return shape[shape.length - 1];
    }

    public boolean isSequenceInput(int tensorNum)
    {
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        return shape.length == 3;
    }

    public int inputSequenceLength(int tensorNum)
    {
        if (!isSequenceInput(tensorNum)) return 1;
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        return shape[shape.length-2];
    }


    public int outputSize(int tensorNum)
    {
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        return shape[shape.length - 1];
    }

    public boolean isSequenceOutput(int tensorNum)
    {
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        return shape.length == 3;
    }

    public int outputSequenceLength(int tensorNum)
    {
        if (!isSequenceOutput(tensorNum)) return 1;
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        return shape[shape.length-2];
    }


    private FloatBuffer makeInputFloatBuffer(int tensorNum)
    {

        int size = inputSize(tensorNum);
        int seq_len = inputSequenceLength(tensorNum);

        return FloatBuffer.allocate(size * seq_len);
    }


    private FloatBuffer makeOutputFloatBuffer(int tensorNum)
    {
        int size = outputSize(tensorNum);
        int seq_len = outputSequenceLength(tensorNum);

        return FloatBuffer.allocate(size * seq_len);
    }



    protected abstract boolean isReady();


    protected abstract void writeInputBuffer(int tensorNum, FloatBuffer buffer);
}
