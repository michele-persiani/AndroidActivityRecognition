package umu.software.activityrecognition.chatbot.impl;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import umu.software.activityrecognition.application.ApplicationSingleton;
import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.services.speech.SpeechService;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;
import umu.software.activityrecognition.shared.util.AndroidUtils;
import umu.software.activityrecognition.shared.util.LogHelper;


/**
 * Wrapper that utters the chatbot's responses and allows to listen from the device's microphone, by using the SpeechService.
 */
public class SpeechChatbot implements Chatbot
{

    private final LogHelper log = LogHelper.newClassTag(this);

    private final Chatbot wrapped;
    private final Context context;
    private boolean autoListen;
    private boolean waitingAnswer = false;
    private ServiceConnectionHandler<SpeechService.SpeechBinder> speechConnection;


    /**
     * Create a new instance
     * @param context context to connect to the SpeechService
     * @param wrapped wrapped chatbot
     */
    public SpeechChatbot(Context context, Chatbot wrapped)
    {
        this.context = context.getApplicationContext();
        this.wrapped = wrapped;
        setAutoListen(true);
    }

    @Override
    public void connect(Consumer<Boolean> successCbk)
    {
        speechConnection = SpeechService.newConnection(context, getLanguage()).bind(SpeechService.class);
        wrapped.connect(successCbk);
    }

    @Override
    public void disconnect(Consumer<Boolean> successCbk)
    {
        wrapped.disconnect(successCbk);
        speechConnection.unbind();
    }

    @Override
    public boolean isConnected()
    {
        return wrapped.isConnected() && speechConnection.isBound();
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
     * Whether is talking, listening or waiting an answer from the server
     * @return true or false
     */
    public boolean isBusy()
    {
        return isWaitingAnswer() || speechConnection.applyBoundFunction(SpeechService.SpeechBinder::isBusy, false);
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

    /**
     * Stop listening
     */
    public synchronized void stopListening()
    {
        speechConnection.applyBound(SpeechService.SpeechBinder::stopListening);
    }

    /**
     * Start listening using SpeechService. The recognized speech is send as a message to the wrapped chatbot
     * @param cbkResponse callback receiving the chatbot's response
     */
    public synchronized void startListening(@Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        if (isBusy())
            return;

        speechConnection.applyBound(binder -> {
            binder.startListening(utterance -> {
                if (utterance == null && cbkResponse != null)
                    cbkResponse.accept(ChatbotResponse.forError("Error from the speech recognizer"));
                else
                    sendMessage(utterance, cbkResponse);
            });
        });
    }

    /**
     * Wraps the callback with another that speaks the chatbot's text answers
     * @param cbkResponse callback to wrap
     * @return wrapped callback
     */
    private Consumer<ChatbotResponse> wrapCallbackWithSay(@Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        waitingAnswer = true;
        return response -> {
            waitingAnswer = false;
            log.i("Received answer: %s", response);
            if (cbkResponse != null) cbkResponse.accept(response);
            if (response != null && response.getAnswerText() != null)
                say(response.getAnswerText(), cbkResponse);
        };
    }


    private void say(String message, Consumer<ChatbotResponse> cbkResponse)
    {
        speechConnection.applyBound(binder -> {
            binder.say(message, result -> {
                if (!result)
                {
                    log.e("TTS error");
                    cbkResponse.accept(ChatbotResponse.forError("TTS error"));
                }
                else if (isAutoListening() && isConnected()) {
                    startListening(cbkResponse);
                }
            });
        });
    }

}
