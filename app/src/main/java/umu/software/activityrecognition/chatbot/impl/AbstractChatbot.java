package umu.software.activityrecognition.chatbot.impl;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;


/**
 * Base class for chatbots that uses an executor to handle messages and events
 */
public abstract class AbstractChatbot implements Chatbot
{
    protected ExecutorService executor;
    private Locale locale = Locale.ENGLISH;



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
            if (!isConnected()) return;
            ChatbotResponse response = processMessage(message.toString());

            if (cbkResponse != null) {
                response.setPromptText(message.toString());
                cbkResponse.accept(response);
            }
        });
    }

    @Override
    public void sendEvent(String name, @Nullable Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse)
    {
        executor.submit( () -> {
            if (!isConnected()) return;
            ChatbotResponse response = processEvent(name, params);

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


    protected abstract ChatbotResponse processEvent(String name, @Nullable Map<String, String> params);


    protected abstract ChatbotResponse processMessage(String message);
}
