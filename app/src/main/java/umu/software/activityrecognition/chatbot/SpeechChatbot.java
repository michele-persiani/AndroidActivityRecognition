package umu.software.activityrecognition.chatbot;

import android.speech.SpeechRecognizer;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import umu.software.activityrecognition.shared.util.LogHelper;
import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.BaseSpeechRecognitionListener;
import umu.software.activityrecognition.speech.TTS;
import umu.software.activityrecognition.speech.translate.LanguageTranslation;


/**
 * Wrapper that utters the chatbot's responses and allows to listen from the device's microphone, by using ASR and TTS.
 * Can specify also a 'voice persona' that allows to talk a language that is different from that of the
 * wrapped chatbot. In this case the recognized speech is automatically translated to the language
 * utilized by the chatbot and vice-versa. For example swedish speaker using an english chatbot
 */
public class SpeechChatbot implements Chatbot
{
    private LogHelper mLog = LogHelper.newClassTag(this);

    private final Chatbot wrapped;

    private boolean autoListen;
    private Locale voiceLanguage;

    private boolean waitingAnswer = false;
    private Voice speechVoice;
    private float speechRate;


    /**
     * Create a new instance
     * @param wrapped wrapped chatbot
     * @param autoListen see setAutoListen()
     */
    public SpeechChatbot(Chatbot wrapped, boolean autoListen)
    {
        this.wrapped = wrapped;
        voiceLanguage = wrapped.getLanguage();
        speechRate = 1.f;
        speechVoice = null;
        setAutoListen(autoListen);
    }

    @Override
    public void connect(Consumer<Boolean> successCbk)
    {
        wrapped.connect(successCbk);
    }

    @Override
    public void disconnect(Consumer<Boolean> successCbk)
    {
        wrapped.disconnect(successCbk);
    }

    @Override
    public boolean isConnected()
    {
        return wrapped.isConnected();
    }

    @Override
    public void sendMessage(CharSequence message, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        if (isBusy() && cbkResponse != null) {
            cbkResponse.accept(ChatbotResponse.forError("Busy with another request"));
            return;
        }
        wrapped.sendMessage(message, wrapCallbackWithSay(cbkResponse));
    }

    @Override
    public void sendEvent(String name, @Nullable Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        if (isBusy() && cbkResponse != null) {
            cbkResponse.accept(ChatbotResponse.forError("Busy with other request"));
            return;
        }
        wrapped.sendEvent(name, params, wrapCallbackWithSay(cbkResponse));
    }

    @Override
    public Locale getLanguage()
    {
        return wrapped.getLanguage();
    }

    @Override
    public void setLanguage(Locale language)
    {
        wrapped.setLanguage(language);
    }


    /**
     * Sets a persona for the voice of the chatbot. The persona's language determines which language
     * the chatbot uses in its speech. This language it then translated to the wrapped chatbot.
     * @param voiceLanguage the language to use for the chatbot's voice
     * @param speechRate the speech rate of the chatnot's voice
     * @param voiceSelector function to select the chatbot's voice. Its returned value must be
     *                     between 0 and (voices.size() - 1)
     */
    public void setVoicePersona(@NonNull Locale voiceLanguage, float speechRate, @Nullable Function<List<Voice>, Integer> voiceSelector)
    {
        this.voiceLanguage = voiceLanguage;
        this.speechRate = speechRate;

        if (voiceSelector == null)
        {
            speechVoice = TTS.INSTANCE.getDefaultVoice();
            return;
        }

        List<Voice> voices = getAvailableVoices();
        if (voices.size() > 0)
        {
            int voiceNum = Math.max(0, Math.min(voices.size() - 1, voiceSelector.apply(voices)));
            speechVoice = voices.get(voiceNum);
        }
    }

    /**
     * Gets the list of voices available for the current language
     * @return the list of voices available for the current language
     */
    public List<Voice> getAvailableVoices()
    {
        return TTS.INSTANCE.getAvailableVoices((v) -> {
            return v.getLocale().equals(voiceLanguage);
        });
    }


    /**
     * Whether isWaitingAnswer(), isListening() or isTalking() is true
     * @return true or false
     */
    public boolean isBusy()
    {
        return isTalking() || isListening() || isWaitingAnswer();
    }

    /**
     * Returns whether this chatbot is waiting for the wrapped chatbot to produce an answer
     * @return true or false
     */
    public boolean isWaitingAnswer()
    {
        return waitingAnswer;
    }

    /**
     * Returns whether the chatbot is listening to user speech
     * @return true or false
     */
    public boolean isListening()
    {
        return ASR.FREE_FORM.isListening();
    }

    /**
     * Returns whether the chatbot is talking
     * @return true or false
     */
    public boolean isTalking()
    {
        return TTS.INSTANCE.isTalking();
    }

    /**
     * Sets whether the chatbot should start listening right after it spoke a response.
     * Useful to create conversation that doesn't require the user to press a button each time before speaking
     * @param value true or false
     */
    public void setAutoListen(boolean value)
    {
        autoListen = value;
    }

    /**
     * See setAutoListen()
     * @return true or false
     */
    public boolean isAutoListening()
    {
        return autoListen;
    }


    public synchronized void stopListening()
    {
        ASR.FREE_FORM.stopListening();
    }

    /**
     * Start listening using ASR. The recognized speech is send as a message to the wrapped chatbot
     * @param cbkResponse callback receiving the chatbot's response
     */
    public synchronized void startListening(@Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        if (isListening())
            return;

        Consumer<ChatbotResponse> finalCbKResponse = (cbkResponse != null)? cbkResponse : response -> {};

        ASR.FREE_FORM.setLanguage(voiceLanguage);
        ASR.FREE_FORM.startListening(new BaseSpeechRecognitionListener()
        {
            @Override
            public void onError(int i)
            {
                mLog.e("ASR error (%s)", i);
                if (i == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    finalCbKResponse.accept(ChatbotResponse.forError("Timeout of speech recognizer"));
                else
                    finalCbKResponse.accept(ChatbotResponse.forError(String.format("Error %s from the speech recognizer", i)));
            }

            @Override
            protected void onRecognizedSpeech(List<Pair<String, Float>> results)
            {
                super.onRecognizedSpeech(results);
                String userInput = results.get(0).first;
                userInput = LanguageTranslation.INSTANCE.getTranslator(voiceLanguage, getLanguage()).translate(userInput);
                sendMessage(userInput, finalCbKResponse);
            }
        });
    }

    /**
     * Wraps the callback with another that uses TTS to speak the chatbot's text answers
     * @param cbkResponse callback to wrap
     * @return wrapped callback
     */
    private Consumer<ChatbotResponse> wrapCallbackWithSay(@Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        waitingAnswer = true;
        return response -> {
            waitingAnswer = false;
            mLog.i("Received answer: %s", response);
            if (cbkResponse != null) cbkResponse.accept(response);
            if (response != null && response.getAnswerText() != null)
                say(response.getAnswerText(), cbkResponse);
        };
    }


    private void say(String message, Consumer<ChatbotResponse> cbkResponse)
    {
        Locale chatbotLanguage = getLanguage();
        message = LanguageTranslation.INSTANCE.getTranslator(chatbotLanguage, voiceLanguage).translate(message);
        TTS.INSTANCE.setLanguage(voiceLanguage);
        TTS.INSTANCE.setVoice(speechVoice);
        TTS.INSTANCE.setSpeechRate(speechRate);
        mLog.i("Saying message: (%s) chatbot_language: (%s) persona_language: (%s) voice: (%s)",
                message,
                chatbotLanguage,
                voiceLanguage,
                speechVoice.getName()
        );
        TTS.INSTANCE.say(message, new UtteranceProgressListener()
        {
            @Override
            public void onDone(String s)
            {
                if (isAutoListening() && isConnected())
                    startListening(cbkResponse);
            }

            @Override
            public void onStart(String s) { }

            @Override
            public void onError(String s)
            {
                String msg = String.format("TTS error: %s", s);
                mLog.e(msg);
                cbkResponse.accept(ChatbotResponse.forError(msg));
            }
        });
    }

}
