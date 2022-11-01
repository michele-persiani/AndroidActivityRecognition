package umu.software.activityrecognition.chatbot.impl.stateful;


import java.util.Map;

import umu.software.activityrecognition.chatbot.ChatbotResponse;


/**
 * Interface for state state transitions inside the dialogue manager
 */
public interface IDialogueTransition
{

    /**
     * Returns whether the transition should reset the previous chatbot response
     * @return whether the transition should reset the previous chatbot response
     */
    boolean resetResponse();

    /**
     * Stateless test if the transitions accepts the given event
     * @param eventName input message to test
     * @param properties global dialogue properties
     * @return whether this transition can be triggered by the event
     */
    boolean testEvent(String eventName, Map<String, String> properties);

    /**
     * Stateless test if the transitions accepts the given message
     * @param input input message
     * @param properties global dialogue properties
     * @return whether this transition can be triggered by the input message
     */
    boolean testMessage(String input, Map<String, String> properties);

    /**
     * Handle an event transition
     * @param eventName
     * @param dialogueState
     * @param properties global dialogue properties
     */
    void onEvent(String eventName, ChatbotResponse dialogueState, Map<String, String> properties);

    /**
     * handle a message transition
     * @param input input message
     * @param response response to build
     * @param properties
     */
    void onMessage(String input, ChatbotResponse response, Map<String, String> properties);

}
