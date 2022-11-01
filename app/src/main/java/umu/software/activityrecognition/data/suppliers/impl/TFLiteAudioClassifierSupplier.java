package umu.software.activityrecognition.data.suppliers.impl;

import android.content.Context;
import android.media.AudioRecord;
import android.util.Log;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;
import org.tensorflow.lite.task.core.BaseOptions;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import umu.software.activityrecognition.data.dataframe.DataFrame;
import umu.software.activityrecognition.data.suppliers.DataSupplier;
import umu.software.activityrecognition.shared.util.Exceptions;


/**
 * DataSupplier to get data from google AudioClassifier
 */
public class TFLiteAudioClassifierSupplier implements DataSupplier
{
    private final Context context;
    private final String modelName;
    private AudioRecord recorder;
    private TensorAudio audioTensor;
    private AudioClassifier classifier;


    public TFLiteAudioClassifierSupplier(Context context, String modelAssetsFileName)
    {
        this.context = context.getApplicationContext();
        modelName = modelAssetsFileName;
    }

    @Override
    public String getName()
    {
        return modelName;
    }

    @Override
    public void initialize()
    {
        AudioClassifier.AudioClassifierOptions options =
                AudioClassifier.AudioClassifierOptions.builder()
                        .setBaseOptions(BaseOptions.builder().build())
                        .build();

        classifier = Exceptions.runCatch(
                () -> AudioClassifier.createFromFileAndOptions(context, modelName, options),
                (AudioClassifier) null
        );

        if (classifier == null)
        {
            Log.e(getClass().getSimpleName(), String.format("Couldn't find a model at path: %s", modelName));
            return;
        }

        recorder = classifier.createAudioRecord();
        audioTensor = classifier.createInputTensorAudio();
        recorder.startRecording();
    }

    @Override
    public boolean isReady()
    {
        return !classifier.isClosed();
    }

    @Override
    public void dispose()
    {
        if(recorder == null)
            return;
        classifier.close();
        recorder.stop();
        recorder = null;
        classifier = null;
    }

    @Override
    public void accept(DataFrame.Row row)
    {
        audioTensor.load(recorder);
        List<Classifications> results = classifier.classify(audioTensor);
        if (results.size() == 0)
            return;
        //Log.i(getClass().getSimpleName(), results.toString());
        results.get(0)
                .getCategories()
                .stream()
                .sorted(Comparator.comparingInt(Category::getIndex))
                .forEach(c -> row.put(c.getLabel(), c.getScore()));
    }


}
