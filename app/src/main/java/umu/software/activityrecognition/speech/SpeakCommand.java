package umu.software.activityrecognition.speech;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import umu.software.activityrecognition.shared.util.BaseBuilder;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;


/**
 * Commands for the TextToSpeech (TTS) singleton
 */
public class SpeakCommand implements Runnable
{

    protected String prompt;
    protected Locale language;
    protected Voice voice;
    protected int queueAction;
    protected float speechRate;
    protected UtteranceProgressListener listener;

    /**
     * Builder for SpeakCommands
     */
    public static class Builder extends BaseBuilder<SpeakCommand>
    {

        /**
         * Sets the listener for the command result. Overrides previously set listeners
         * @param listener listener that will receive callback calls
         * @return
         */
        public Builder setListener(UtteranceProgressListener listener)
        {
            return setFields(this, c -> c.listener = listener);
        }

        /**
         * Sets the listener for the command result. Overrides previously set listeners
         * @param resultListener listener that will receive either true or false depending on the command's success
         * @return
         */
        public Builder setResultListener(Consumer<Boolean> resultListener)
        {
            return setListener(new UtteranceProgressListener()
            {
                @Override
                public void onStart(String utteranceId)
                {

                }

                @Override
                public void onDone(String utteranceId)
                {
                    resultListener.accept(true);
                }

                @Override
                public void onError(String utteranceId)
                {
                    resultListener.accept(false);
                }
            });
        }

        /**
         * Sets what is to be said
         * @param prompt what the command will say
         * @return
         */
        public Builder setPrompt(String prompt)
        {
            return setFields(this, c -> c.prompt = prompt);
        }


        /**
         * Sets the prompt's language
         * @param language language to use
         * @return
         */
        public Builder setLanguage(Locale language)
        {
            return setFields(this, c -> c.language = Locale.forLanguageTag(language.toLanguageTag()));
        }

        /**
         * Sets the voice to use. The voice's language should match the voice set through setLanguage()
         * @param voice voice to use
         * @return
         */
        public Builder setVoice(Voice voice)
        {
            return setFields(this, c -> c.voice = voice);
        }


        public void setVoice(String voiceName)
        {
            List<Voice> voices = TTS.getInstance().getAvailableVoices(null);
            for (Voice v : voices)
                if(v.getName().equals(voiceName))
                    setVoice(v);
        }

        /**
         * Sets what should happen to the command queue when queuing this command. See TextToSpeech.QUEUE_* constants
         * @param queueAction action to perform on the command queue
         * @return
         */
        public Builder setQueueAction(int queueAction)
        {
            return setFields(this, c -> c.queueAction = queueAction);
        }


        /**
         * Sets the voice speech rate
         * @param speechRate speech rate between 0 and 1
         * @return
         */
        public Builder setSpeechRate(float speechRate)
        {
            return setFields(this, c -> c.speechRate = Math.max(0, Math.min(1, speechRate)));
        }



        @Override
        protected SpeakCommand newInstance()
        {
            SpeakCommand cmd = new SpeakCommand();
            cmd.language = Locale.ENGLISH;
            cmd.queueAction = TextToSpeech.QUEUE_FLUSH;
            cmd.speechRate = 1.f;
            cmd.voice = TTS.getInstance().getDefaultVoice();
            return cmd;
        }
    }

    @Override
    public void run()
    {
        if (prompt == null)
            return;
        TTS.getInstance().say(this);

    }
}
