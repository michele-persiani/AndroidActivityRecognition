package umu.software.activityrecognition.chatbot.impl.stateful;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import umu.software.activityrecognition.chatbot.ChatbotResponse;


/**
 * Factory for common DialogState objects
 */
public class DialogueStateFactory
{
    private DialogueStateFactory() {}

    /**
     * Build a new state triggered by an event
     * @param eventName name of the event that can trigger this state
     * @param resetResponse whther the state creates a new dialogue state when triggered
     * @param responseBuilder response builder
     * @return a new DialogueState
     */
    public static DialogueState newEventTriggeredState(String eventName, boolean resetResponse, BiConsumer<String, ChatbotResponse> responseBuilder)
    {
        return new DialogueState()
        {
            @Override
            public boolean resetResponse()
            {
                return resetResponse;
            }

            @Override
            public boolean testEvent(String evt)
            {
                return evt.equals(eventName);
            }

            @Override
            public void onEvent(String eventName, ChatbotResponse dialogueState)
            {
                super.onEvent(eventName, dialogueState);
                responseBuilder.accept(eventName, dialogueState);
            }
        };
    }

    /**
     * Creates a new DialogueState triggered by a message
     * @param messageFilter filter to decide whether the message triggers the state
     * @param resetResponse whether the states resets the state of the dialogue
     * @param responseBuilder response builder
     * @return a newly created DialogueState
     */
    public static DialogueState newMessageTriggeredState(Predicate<String> messageFilter, boolean resetResponse, BiConsumer<String, ChatbotResponse> responseBuilder)
    {
        return new DialogueState()
        {
            @Override
            public boolean resetResponse()
            {
                return resetResponse;
            }
            @Override
            public boolean testMessage(String input)
            {
                return super.testMessage(input) && messageFilter.test(input);
            }

            @Override
            public void onMessage(String input, ChatbotResponse dialogueState)
            {
                super.onMessage(input, dialogueState);
                responseBuilder.accept(input, dialogueState);
            }
        };
    }

    /**
     * Creates a new DialogueState triggered if the input message is similar to a high enough degree
     * to 'words', by the means of JaccardSimilarity computed on bag iof words
     * @param words tested bag of words
     * @param minSimilarity a value between 0 and 1 used to test if the Jaccard similarity between bag of words
     *                      is high enough
     * @param resetResponse wehther the state should reset the dialogue state
     * @param responseBuilder response builder
     * @return a newly created DialogueState
     */
    public static DialogueState newJaccardSimilarityMessageTriggeredState(String[] words, double minSimilarity, boolean resetResponse, BiConsumer<String, ChatbotResponse> responseBuilder)
    {
        Predicate<String> filter = (message) -> {

            Set<String> messageSet0 = Sets.newHashSet(message.split(" "));
            Set<String> messageSet1 = Sets.newHashSet(message.split(" "));
            Set<String> wordsSet = Sets.newHashSet(words);

            messageSet0.addAll(wordsSet);
            messageSet1.retainAll(wordsSet);

            double similarity = (double) messageSet1.size() / (double) messageSet0.size();

            return similarity >= minSimilarity;
        };

        return newMessageTriggeredState(filter, resetResponse, responseBuilder);
    }
}
