package umu.software.activityrecognition.chatbot;


import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for chatbots
 */
public interface Chatbot
{
    /**
     * Connect to the chatbot.
     * @param successCbk optional callback
     */
    void connect(@Nullable Consumer<Boolean> successCbk);

    /**
     * Disconnect from the chatbot
     * @param successCbk optional callback
     */
    void disconnect(@Nullable Consumer<Boolean> successCbk);

    /**
     * Returns whether the chatbot is connected.
     * @return whether the chatbot is connected.
     */
    boolean isConnected();

    /**
     * Sends a message
     * @param message string message to send
     * @param cbkResponse optional callback handling the ChatbotResponse
     */
    void sendMessage(CharSequence message, @Nullable Consumer<ChatbotResponse> cbkResponse);

    /**
     * Sends an event
     * @param name name of th eevent
     * @param params event's parameters
     * @param cbkResponse optional callback fot hthe chatbot's response
     */
    void sendEvent(String name, @Nullable Map<String, String> params, @Nullable Consumer<ChatbotResponse> cbkResponse);

    /**
     * Gets the chatbot's language
     * @return the chatbot's language
     */
    Locale getLanguage();

    /**
     * Sets the chatbot's language
     * @param language the chatbot's language to use
     */
    void setLanguage(Locale language);
}
