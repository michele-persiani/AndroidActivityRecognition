package umu.software.activityrecognition.chatbot.impl.stateful;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.function.Consumer;

import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.chatbot.impl.AbstractChatbot;


/**
 * Chatbot implemented through Finite State Machine.
 *
 * See DialogueFSM()
 */
public class FSMChatbot extends AbstractChatbot
{

    private DialogueFSM stateMachine;
    private Consumer<DialogueFSM> builder;


    public FSMChatbot()
    {
        stateMachine = new DialogueFSM();
    }


    public void setFSMBuilder(Consumer<DialogueFSM> builder)
    {
        this.builder = builder;
    }


    @Override
    public void connect(@Nullable Consumer<Boolean> successCbk)
    {
        super.connect(successCbk);
        stateMachine = new DialogueFSM();
        if (builder != null)
            builder.accept(stateMachine);
        stateMachine.initializeIfNeeded();
    }

    @Override
    public void disconnect(@Nullable Consumer<Boolean> successCbk)
    {
        super.disconnect(successCbk);
        stateMachine = null;
    }

    @Override
    public boolean isConnected()
    {
        return super.isConnected() && stateMachine != null;
    }

    @Override
    protected ChatbotResponse processEvent(String name, @Nullable Map<String, String> params)
    {
        return stateMachine.onEvent(name);
    }

    @Override
    protected ChatbotResponse processMessage(String message)
    {
        return stateMachine.onMessage(message);
    }
}
