package umu.software.activityrecognition.tflite.model;


import com.google.common.collect.Maps;

import org.tensorflow.lite.Interpreter;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import umu.software.activityrecognition.common.FunctionLock;

/**
 * Template for invoking a Tensorflow Lite model. For the moment it supports only
 * input-outputs in the form of sequences, so with tensor shapes of 2 dimensions. eg (seq_len, num_features)
 */
public abstract class TFModel
{
    protected final Interpreter interpreter;
    private final FunctionLock lock = FunctionLock.make();

    private FloatBuffer[] inputs;
    private HashMap<Integer, FloatBuffer> outputs;

    /**
     *
     * @param interpreter a Tensorflow Lite Interpreter
     */
    public TFModel(Interpreter interpreter)
    {
        this.interpreter = interpreter;
        makeBuffers();
    }

    /**
     *
     * @return number of input tensors
     */
    public int getInputTensorCount()
    {
        return interpreter.getInputTensorCount();
    }


    /**
     *
     * @return number of output tensors
     */
    public int getOutputTensorCount()
    {
        return interpreter.getOutputTensorCount();
    }

    /**
     * Make the input/output buffers
     */
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


    /**
     * Create a FloatBuffer for the specified input tensor
     * @param tensorNum input tensor num
     * @return the created FloatBuffer
     */
    private FloatBuffer makeInputFloatBuffer(int tensorNum)
    {

        int size = inputSize(tensorNum);
        int seq_len = inputSequenceLength(tensorNum);

        return FloatBuffer.allocate(size * seq_len);
    }


    /**
     * Create a FloatBuffer for the specified output tensor
     * @param tensorNum output tensor num
     * @return the created FloatBuffer
     */
    private FloatBuffer makeOutputFloatBuffer(int tensorNum)
    {
        int size = outputSize(tensorNum);
        int seq_len = outputSequenceLength(tensorNum);

        return FloatBuffer.allocate(size * seq_len);
    }

    /**
     * Predict using the Tensorflow model.
     * The result of the prediction can later be accessed through getOutput()
     * @return whether the invocation of the model was successful
     */
    public boolean predict()
    {
        int inputsCount = interpreter.getInputTensorCount();
        int outputsCount = interpreter.getOutputTensorCount();

        if (!isInputReady())
            return false;

        for (int i = 0; i < inputsCount; i++)
        {
            FloatBuffer buffer  = inputs[i];
            buffer.rewind();
            writeInputBuffer(i, buffer);
            buffer.rewind();
        }

        Map<Integer, Object> outMap = Maps.newHashMap();

        lock.withLock(() -> {
            for (int i = 0; i < outputsCount; i++)
            {
                FloatBuffer buffer  = outputs.get(i);
                buffer.rewind();
                outMap.put(i, buffer);
            }
            interpreter.runForMultipleInputsOutputs(inputs, outMap);
        });
        return true;
    }

    /**
     * Get the nth output as a sequence of vectors. To be called after a successful predict() to read
     * the outputs
     * @param outputNum the output of the model to fetch
     * @return the value of the nth output of the model as a sequence of vectors
     */
    public float[][] getOutput(int outputNum)
    {
       assert 0 >= outputNum && outputNum < interpreter.getOutputTensorCount();

        FloatBuffer buffer = lock.withLock(() -> outputs.get(outputNum).duplicate());

        int seqLen = outputSequenceLength(outputNum);
        int outSize = outputSize(outputNum);
        float[][] res = new float[seqLen][outSize];

        buffer.rewind();
        for (int i=0; i < seqLen;i++)
            for (int j = 0; j < outSize; j++)
                res[i][j] = buffer.get();


        return res;
    }


    /**
     * Size of the nth input tensor
     * @param tensorNum number of the tensor
     * @return its size as the number of features of the last dimension
     */
    public int inputSize(int tensorNum)
    {
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        return shape[shape.length - 1];
    }

    /**
     * Whether the nth input is of sequences. That is if its shape is (batch_size, seq_length, num_features)
     * @param tensorNum number of the tensor
     * @return whether the specified tensor is of sequences
     */
    public boolean isSequenceInput(int tensorNum)
    {
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        assert shape.length <= 3;
        return shape.length == 3;
    }

    /**
     * The length of the sequences for the nth input tensor. Will return 1 if the input is not a sequence
     * @param tensorNum  number of the tensor
     * @return length of the input sequences
     */
    public int inputSequenceLength(int tensorNum)
    {
        if (!isSequenceInput(tensorNum)) return 1;
        int[] shape = interpreter.getInputTensor(tensorNum).shape();
        if (shape.length > 3)
            throw new RuntimeException(String.format("Shape %s is not supported", Arrays.toString(shape)));
        return shape[shape.length-2];
    }

    /**
     * Size of the nth output tensor in terms of number of features
     * @param tensorNum number of the tensor
     * @return the tensor size
     */
    public int outputSize(int tensorNum)
    {
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        return shape[shape.length - 1];
    }

    /**
     * Whether the nth output is of sequences. That is if its shape is (batch_size, seq_length, num_features)
     * @param tensorNum number of the tensor
     * @return whether the specified tensor is of sequences
     */
    public boolean isSequenceOutput(int tensorNum)
    {
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        if (shape.length > 3)
            throw new RuntimeException(String.format("Shape %s is not supported", Arrays.toString(shape)));
        return shape.length == 3;
    }

    /**
     * The length of the sequences for the nth output tensor
     * @param tensorNum  number of the tensor
     * @return length of the input sequences
     */
    public int outputSequenceLength(int tensorNum)
    {
        if (!isSequenceOutput(tensorNum)) return 1;
        int[] shape = interpreter.getOutputTensor(tensorNum).shape();
        return shape[shape.length-2];
    }



    /**
     * Tests whether the underlying implementation is ready to supplying all the inputs to the Tensorflow Lite model
     * @return true/false
     */
    protected abstract boolean isInputReady();

    /**
     * Write input buffer for the specified input tensor. Always called after isInputReady()
     * @param tensorNum the tensor num
     * @param buffer the tensor's buffer to be written
     */
    protected abstract void writeInputBuffer(int tensorNum, FloatBuffer buffer);


}
