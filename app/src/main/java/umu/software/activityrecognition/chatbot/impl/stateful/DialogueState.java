package umu.software.activityrecognition.chatbot.impl.stateful;


import umu.software.activityrecognition.chatbot.ChatbotResponse;

public class DialogueState
{

    public boolean resetResponse()
    {
        return true;
    }

    /**
     * Stateless test
     * @param eventName input message to test
     * @return whether this state should be triggered by the event
     */
    public boolean testEvent(String eventName)
    {
        return true;
    }

    /**
     * Stateless test
     * @param input
     * @return whether this state should be triggered by the input message
     */
    public boolean testMessage(String input)
    {
        return true;
    }

    /**
     * Create
     * @param eventName
     * @param dialogueState
     */
    public void onEvent(String eventName, ChatbotResponse dialogueState)
    {

    }

    public void onMessage(String input, ChatbotResponse dialogueState)
    {

    }

}
