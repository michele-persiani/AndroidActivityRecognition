package umu.software.activityrecognition.chatbot.impl.classifier;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.DoubleStream;



public class EmbeddingClass
{
    private final double[] compareVector;
    private final Map<String, double[]> embeddingMatrix;

    public EmbeddingClass(String [] words, Map<String, double[]> embeddingMatrix)
    {
        assert embeddingMatrix.size() > 0;
        this.embeddingMatrix = embeddingMatrix;
        this.compareVector = average(words);
    }

    public int getEmbeddingSize()
    {
        return embeddingMatrix.values().iterator().next().length;
    }

    public double[] getVector()
    {
        return compareVector;
    }

    /**
     *
     * @param text query text to compare with the wrapped text
     * @return a value between 0 and 1 indicating the similarity with the wrapped text
     */
    public double similarity(String text)
    {
        String[] query = text.split(" ");

        if (query.length == 0)
            return 0;

        double[] queryVector = average(query);

        double dotSim = dotSimilarity(queryVector, compareVector);

        return (dotSim + 1) / 2;
    }

    private double[] average(String[] words)
    {
        int embSize = getEmbeddingSize();
        double[] average = new double[embSize];
        double count = 0;
        for (String w : words)
        {
            if (!embeddingMatrix.containsKey(w)) continue;
            count ++;
            double[] embedding = embeddingMatrix.get(w);
            for (int i = 0; i < embSize; i++)
                average[i] += embedding[i];
        }
        for (int i = 0; i < embSize; i++)
            average[i] /= count;
        return average;
    }

    private double dotSimilarity(double[] v0, double[] v1)
    {
        assert v0.length == v1.length;
        double m0 = Math.sqrt(DoubleStream.of(v0).map(v -> Math.pow(v, 2)).sum());
        double m1 = Math.sqrt(DoubleStream.of(v1).map(v -> Math.pow(v, 2)).sum());

        double vv = 0;
        for (int i = 0; i < v0.length; i++)
            vv += v0[i] * v1[i];
        vv /= m0 * m1;
        return vv;
    }

}
