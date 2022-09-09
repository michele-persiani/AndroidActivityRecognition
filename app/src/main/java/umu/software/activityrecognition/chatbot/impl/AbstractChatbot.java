package umu.software.activityrecognition.chatbot.impl;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;

public abstract class AbstractChatbot implements Chatbot
{
    private ExecutorService executor;
    private Locale locale = Locale.getDefault();

    @Override
    public void connect(@Nullable Consumer<Boolean> successCbk)
    {
        if (executor != null)
            executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void disconnect(@Nullable Consumer<Boolean> successCbk)
    {
        executor.shutdownNow();
        executor = null;
    }

    @Override
    public boolean isConnected()
    {
        return executor != null;
    }

    @Override
    public void sendMessage(CharSequence message, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        executor.submit( () -> {
            ChatbotResponse response = new ChatbotResponse();
            response.setPromptText(message.toString());
            processMessage(message.toString(), response);
            if (cbkResponse != null)
                cbkResponse.accept(response);
        });
    }

    @Override
    public void sendEvent(String name, @Nullable Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        executor.submit( () -> {
            ChatbotResponse response = new ChatbotResponse();
            processEvent(name, params, response);
            if (cbkResponse != null)
                cbkResponse.accept(response);
        });
    }

    @Override
    public Locale getLanguage()
    {
        return Locale.forLanguageTag(locale.toLanguageTag());
    }

    @Override
    public void setLanguage(Locale language)
    {
        locale = language;
    }

    protected abstract void processEvent(String name, @Nullable Map<String, String> params, ChatbotResponse response);


    protected abstract void processMessage(String message, ChatbotResponse response);
}
