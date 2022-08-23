package umu.software.activityrecognition.speech;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.api.client.util.Lists;

import java.util.Arrays;
import java.util.List;

public class BaseSpeechRecognitionListener implements RecognitionListener
{

    protected void onRecognizedSpeech(List<Pair<String, Float>> results)
    {
    }

    protected void onRecognizedPartialSpeech(List<Pair<String, Float>> results)
    {
    }


    // Base methods
    @Override
    public void onReadyForSpeech(Bundle bundle)
    {

    }

    @Override
    public void onBeginningOfSpeech()
    {

    }

    @Override
    public void onRmsChanged(float v)
    {

    }

    @Override
    public void onBufferReceived(byte[] bytes)
    {

    }

    @Override
    public void onEndOfSpeech()
    {

    }

    @Override
    public void onError(int i)
    {

    }

    @Override
    public void onResults(Bundle bundle)
    {
        List<Pair<String, Float>> results = getResults(bundle);
        if (results != null)
            onRecognizedSpeech(results);
    }

    @Override
    public void onPartialResults(Bundle bundle)
    {
        List<Pair<String, Float>> results = getResults(bundle);
        if (results != null)
            onRecognizedPartialSpeech(results);
    }

    @Override
    public void onEvent(int i, Bundle bundle)
    {

    }

    @Nullable
    private List<Pair<String, Float>> getResults(@NonNull Bundle bundle)
    {
        if (!bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION))
            return null;
        List<String> speech = ASR.getRecognizedSpeech(bundle);
        float[] scores = ASR.getConfidenceScores(bundle);
        if (scores == null)
        {
            scores = new float[speech.size()];
            Arrays.fill(scores, 0f);
        }
        List<Pair<String, Float>> results = Lists.newArrayList();

        for (int i = 0; i < speech.size(); i++)
            results.add(Pair.create(speech.get(i), scores[i]));
        return results;
    }
}
