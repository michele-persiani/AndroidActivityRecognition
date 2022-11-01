package umu.software.activityrecognition.speech;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.util.Pair;
import umu.software.activityrecognition.shared.util.BaseBuilder;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ListenCommand implements Runnable
{
    protected Intent recognizerIntent;
    protected RecognitionListener listener;


    public static class Builder extends BaseBuilder<ListenCommand>
    {
        public Builder setCompleteSilenceMillis(long completeSilenceMillis)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilenceMillis));
        }


        public Builder setMinimumSpeechLength(long minimumLengthMillis)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minimumLengthMillis));
        }


        public Builder setPreferOffline(boolean preferOffline)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline));
        }


        public Builder setMaxResults(int maxResults)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults));
        }


        public Builder setLanguage(Locale locale)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale));
        }


        public Builder setLanguageModel(String model)
        {
            return setFields(this, c -> c.recognizerIntent.putExtra(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, model));
        }

        public Builder setResultListener(Consumer<List<Pair<String, Float>>> listener)
        {
            return setFields(this, c -> c.listener = new BaseSpeechRecognitionListener()
            {
                @Override
                protected void onRecognizedSpeech(List<Pair<String, Float>> results)
                {
                    super.onRecognizedSpeech(results);
                    listener.accept(results);
                }
            });
        }

        public Builder setListener(RecognitionListener listener)
        {
            return setFields(this, c -> c.listener = listener);
        }




        @Override
        protected ListenCommand newInstance() {
            ListenCommand cmd = new ListenCommand();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, new String[]{});
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            cmd.recognizerIntent = intent;
            return cmd;
        }
    }

    @Override
    public void run()
    {
        ASR.getInstance().startListening(this);
    }
}
