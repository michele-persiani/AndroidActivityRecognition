package umu.software.activityrecognition.chatbot.impl.stateful;


import java.util.Map;

import umu.software.activityrecognition.chatbot.ChatbotResponse;

public class BaseDialogueTransition implements IDialogueTransition
{

    /**
     * Whether the transition should reset the previous chatbot response
     * @return
     */
    public boolean resetResponse()
    {
        return true;
    }

    /**
     * Stateless test
     * @param eventName input message to test
     * @param properties global dialogue properties
     * @return whether this transition can be triggered by the event
     */
    public boolean testEvent(String eventName, Map<String, String> properties)
    {
        return false;
    }

    /**
     * Stateless test
     * @param input
     * @param properties global dialogue properties
     * @return whether this transition can be triggered by the input message
     */
    public boolean testMessage(String input, Map<String, String> properties)
    {
        return false;
    }

    /**
     * Handle an event transition
     * @param eventName
     * @param dialogueState
     * @param properties global dialogue properties
     */
    public void onEvent(String eventName, ChatbotResponse dialogueState, Map<String, String> properties)
    {

    }

    public void onMessage(String input, ChatbotResponse dialogueState, Map<String, String> properties)
    {

    }

}
