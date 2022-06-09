package umu.software.activityrecognition.chatbot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class ChatbotResponse
{
    private String promptText;
    private String answerText;
    private String intent;
    private String action;


    private final Map<String, String> slots = Maps.newHashMap();


    private Exception exception;


    /**
     *
     * @return the text that triggered the bot's response
     */
    public String getPromptText()
    {
        return promptText;
    }


    public ChatbotResponse setPromptText(String promptText)
    {
        this.promptText = promptText;
        return this;
    }

    /**
     *
     * @return the bot's textual answer to the prompt
     */
    public String getAnswerText()
    {
        return answerText;
    }


    public ChatbotResponse setAnswerText(String answerText)
    {
        this.answerText = answerText;
        return this;
    }

    /**
     * Get the intent found in the prompt
     * @return
     */
    public String getIntent()
    {
        return intent;
    }

    /**
     * Sets the intent found in the prompt
     * @param intent the intent found in the prompt
     */
    public ChatbotResponse setIntent(String intent)
    {
        this.intent = intent;
        return this;
    }

    /**
     *
     * @return names of the slots found in the prompts
     */
    public List<String> getSlotsName()
    {
        return Lists.newArrayList(slots.keySet());
    }

    /**
     *
     * @param slotName the name of the slot to get
     * @return the value of the provided slot, or null if the given slot does not exists
     */
    public String getSlot(String slotName)
    {
        return slots.getOrDefault(slotName, null);
    }

    public boolean hasSlots(String... slotNames)
    {
        for (String n : slotNames)
            if (!slots.containsKey(n))
                return false;
        return true;
    }

    public ChatbotResponse setSlot(String name, String value)
    {
        slots.put(name, value);
        return this;
    }

    /**
     *
     * @return the action associated fith the found intent
     */
    public String getAction()
    {
        return action;
    }


    public ChatbotResponse setAction(String action)
    {
        this.action = action;
        return this;
    }


    public boolean allSlotsSpecified()
    {
        if (slots.size() == 0) return true;
        for (String s : slots.keySet())
            if (getSlot(s) == null)
                return false;
        return true;
    }

    public boolean hasError()
    {
        return exception != null;
    }

    public ChatbotResponse setException(Exception e)
    {
        exception = e;
        return this;
    }


    public Exception getError()
    {
        return exception;
    }


    public static ChatbotResponse forError(String message)
    {
        return new ChatbotResponse().setException(new RuntimeException(message));
    }


    public static ChatbotResponse forError(Exception e)
    {
        return new ChatbotResponse().setException(e);
    }

    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : getSlotsName())
            stringBuilder.append(String.format("%s=%s, ", s, String.format("'%s'", getSlot(s))));

        String slotsString = getSlotsName().size() == 0? "" : stringBuilder.substring(0, stringBuilder.length() - 2);


        return String.format("ChatbotResponse{prompt : {%s}, answer : {%s}, intent : {%s}, action : {%s}, slots : {%s}, exception : {%s}}",
                getPromptText(),
                getAnswerText(),
                getIntent(),
                getAction(),
                slotsString,
                getError()
                );
    }
}
