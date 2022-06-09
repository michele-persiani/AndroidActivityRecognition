package umu.software.activityrecognition.chatbot;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.function.Consumer;

public class ChatbotActionConsumer implements Consumer<ChatbotResponse>
{
    private Consumer<ChatbotResponse> mWrapped;
    private final Map<String, Consumer<ChatbotResponse>> consumers = Maps.newHashMap();

    public ChatbotActionConsumer(Consumer<ChatbotResponse> wrapped)
    {
        mWrapped = wrapped;
    }

    public ChatbotActionConsumer()
    {
        this(null);
    }


    public void setWrapped(Consumer<ChatbotResponse> wrapped)
    {
        mWrapped = wrapped;
    }

    @Override
    public void accept(ChatbotResponse chatbotResponse)
    {
        if (mWrapped != null)
            mWrapped.accept(chatbotResponse);
        String action = chatbotResponse.getAction();
        if(action != null && consumers.containsKey(action))
            consumers.get(action).accept(chatbotResponse);
    }

    public ChatbotActionConsumer setActionConsumer(String action, Consumer<ChatbotResponse> consumer)
    {
        consumers.put(action, consumer);
        return this;
    }

}
