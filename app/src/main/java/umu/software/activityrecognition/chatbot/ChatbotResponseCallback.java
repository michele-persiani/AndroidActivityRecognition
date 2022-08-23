package umu.software.activityrecognition.chatbot;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Decorator that forwards the received messages to an external callback
 */
public class ChatbotResponseCallback implements Chatbot
{
    private final Chatbot wrapped;
    private final Consumer<ChatbotResponse> callback;

    /**
     *
     * @param wrapped wrapped chatbot implementation
     * @param callback callback that will receive the messages sent by the wrapped chatbot
     */
    public ChatbotResponseCallback(Chatbot wrapped, Consumer<ChatbotResponse> callback)
    {
        this.wrapped = wrapped;
        this.callback = callback;
    }


    @Override
    public void connect(@Nullable Consumer<Boolean> successCbk)
    {
        wrapped.connect(successCbk);
    }

    @Override
    public void disconnect(@Nullable Consumer<Boolean> successCbk)
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
        wrapped.sendMessage(message, (response -> {
            if (cbkResponse != null)
                cbkResponse.accept(response);
            callback.accept(response);
        }));
    }

    @Override
    public void sendEvent(String name, Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        wrapped.sendEvent(name, params, (response -> {
            if (cbkResponse != null)
                cbkResponse.accept(response);
            callback.accept(response);
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

}
