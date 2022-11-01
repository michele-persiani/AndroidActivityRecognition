package umu.software.activityrecognition.chatbot.impl.stateful;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.chatbot.impl.AbstractChatbot;
import umu.software.activityrecognition.shared.util.FunctionLock;


/**
 * Chatbot implemented through Finite State Machine.
 *
 * See DialogueFSM()
 */
public class StateMachineChatbot extends AbstractChatbot
{

    private DialogueStateMachine stateMachine;

    private Timer resetTimer;

    private final FunctionLock lock = FunctionLock.newInstance();


    public StateMachineChatbot()
    {
        stateMachine = new DialogueStateMachine();
    }


    public void setDialogueStateMachine(DialogueStateMachine stateMachine)
    {
        this.stateMachine = stateMachine;
    }


    @Override
    public void connect(@Nullable Consumer<Boolean> successCbk)
    {
        super.connect(successCbk);
        stateMachine.initializeIfNeeded();
        stateMachine.resetToInitialState();
    }

    @Override
    public boolean isConnected()
    {
        return super.isConnected() && stateMachine != null;
    }


    @Override
    protected ChatbotResponse processEvent(String name, @Nullable Map<String, String> params)
    {
        return lock.withLock(() -> {
            setResetTimer();
            return stateMachine.onEvent(name);
        });
    }

    @Override
    protected ChatbotResponse processMessage(String message)
    {
        return lock.withLock(() -> {
            setResetTimer();
            return stateMachine.onMessage(message);
        });
    }


    private void setResetTimer()
    {
        if (resetTimer != null)
        {
            resetTimer.cancel();
            resetTimer = null;
        }

        long millis = 1000L * stateMachine.getProperty("resetAfter", 10, Integer::parseInt);

        resetTimer = new Timer();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run()
            {
                stateMachine.onReset();
                resetTimer = null;
            }
        }, millis);
    }

}
