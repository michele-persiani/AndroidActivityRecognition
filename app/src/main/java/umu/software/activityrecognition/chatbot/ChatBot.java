package umu.software.activityrecognition.chatbot;


import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;


public interface ChatBot
{
    void connect(Consumer<Boolean> successCbk);
    void disconnect(Consumer<Boolean> successCbk);
    boolean isConnected();
    void sendMessage(CharSequence message, Consumer<ChatbotResponse> cbkResponse);
    void sendEvent(String name, Map<String, String> params, Consumer<ChatbotResponse> cbkResponse);
    Locale getLanguage();
    void setLanguage(Locale language);
}
