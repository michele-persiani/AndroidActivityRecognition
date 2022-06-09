package umu.software.activityrecognition.chatbot;

import android.speech.SpeechRecognizer;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.util.Pair;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import umu.software.activityrecognition.speech.ASR;
import umu.software.activityrecognition.speech.BaseRecognitionListener;
import umu.software.activityrecognition.speech.TTS;
import umu.software.activityrecognition.speech.Translator;

public class SpeechChatbot implements ChatBot
{

    private final ChatBot wrapped;

    private boolean autoListen;
    private Translator sourceDestTranslator;
    private Translator destSourceTranslator;

    private List<Voice> voices;

    private boolean waitingAnswer = false;


    public SpeechChatbot(ChatBot wrapped, boolean autoListen)
    {
        this.wrapped = wrapped;
        setAutoListen(autoListen);
    }

    public SpeechChatbot(ChatBot wrapped)
    {
        this(wrapped, false);
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
    public void sendMessage(CharSequence message, Consumer<ChatbotResponse> cbkResponse)
    {
        if (isBusy()) {
            cbkResponse.accept(ChatbotResponse.forError("Busy with another request"));
            return;
        }
        waitingAnswer = true;
        wrapped.sendMessage(message, (response -> {
            waitingAnswer = false;
            Log.d(getClass().getSimpleName(),
                    String.format("Received answer for message '%s': %s", message, response)
            );
            cbkResponse.accept(response);
            if (response != null && response.getAnswerText() != null)
                say(response.getAnswerText(), cbkResponse);
        }));
    }

    @Override
    public void sendEvent(String name, Map<String, String> params, Consumer<ChatbotResponse> cbkResponse)
    {
        if (isBusy()) {
            cbkResponse.accept(ChatbotResponse.forError("Busy with other request"));
            return;
        }
        waitingAnswer = true;
        wrapped.sendEvent(name, params, (response -> {
            waitingAnswer = false;
            Log.d(getClass().getSimpleName(),
                    String.format("Received answer for event '%s': %s", name, response)
            );
            cbkResponse.accept(response);
            if (response != null && response.getAnswerText() != null)
                say(response.getAnswerText(), cbkResponse);
        }));
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
     * the chatbot uses in its speech. This language it then translated to the chatbot's wrapped one.
     * @param voiceLanguage the language to use for the chatbot's voice
     * @param speechRate the speech rate of the chatnot's voice
     */
    public void setVoicePersona(@NonNull Locale voiceLanguage, float speechRate)
    {
        sourceDestTranslator = Translator.newInstance(voiceLanguage, getLanguage());
        destSourceTranslator = Translator.invertSourceDest(sourceDestTranslator);
        List<Voice> voices = TTS.INSTANCE.getAvailableVoices((v) -> v.getLocale().getLanguage().equals(voiceLanguage.getLanguage()));
        if (voices.size() > 0)
            TTS.INSTANCE.setVoice(
                    TTS.INSTANCE.getAvailableVoices((v) -> v.getLocale().getLanguage().equals(voiceLanguage.getLanguage())).get(0)
            );
        ASR.FREE_FORM.setLanguage(voiceLanguage);
        TTS.INSTANCE.setSpeechRate(speechRate);
    }


    public boolean isBusy()
    {
        return isTalking() || isListening() || isWaitingAnswer();
    }


    public boolean isWaitingAnswer()
    {
        return waitingAnswer;
    }


    public boolean isListening()
    {
        return ASR.FREE_FORM.isListening();
    }


    public boolean isTalking()
    {
        return TTS.INSTANCE.isTalking();
    }

    /**
     * Sets whether the chatbot should start listening right after it uttered a response.
     * Useful to create conversation that doesn't require the user to press a button before speaking
     * @param value
     */
    public void setAutoListen(boolean value)
    {
        autoListen = value;
    }


    public boolean isAutoListening()
    {
        return autoListen;
    }


    public synchronized void stopListening()
    {
        ASR.FREE_FORM.stopListening();
    }


    public synchronized void startListening(Consumer<ChatbotResponse> cbkResponse)
    {
        if (isListening())
            return;

        if (voices == null)
            voices = TTS.INSTANCE.getAvailableVoices(null);

        ASR.FREE_FORM.startListening(sourceDestTranslator, new BaseRecognitionListener()
        {
            @Override
            public void onError(int i)
            {
                if (i == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    cbkResponse.accept(
                            ChatbotResponse.forError("Timeout of speech recognizer")
                    );
                cbkResponse.accept(
                        ChatbotResponse.forError(String.format("Error %s from the speech recognizer", i))
                );
            }

            @Override
            protected void onRecognizedSpeech(List<Pair<String, Float>> results)
            {
                super.onRecognizedSpeech(results);
                String userInput = results.get(0).first;
                sendMessage(userInput, cbkResponse);
            }
        });
    }

    private void say(String message, Consumer<ChatbotResponse> cbkResponse)
    {
        TTS.INSTANCE.say(message, destSourceTranslator, new UtteranceProgressListener()
        {
            @Override
            public void onDone(String s)
            {
                if (isAutoListening())
                    startListening(cbkResponse);
            }

            @Override
            public void onStart(String s) { }

            @Override
            public void onError(String s) { }
        });
    }

}
