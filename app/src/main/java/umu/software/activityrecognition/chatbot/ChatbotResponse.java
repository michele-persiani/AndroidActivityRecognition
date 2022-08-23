package umu.software.activityrecognition.chatbot;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * Response from a Chatbot
 */
public class ChatbotResponse implements Serializable, Parcelable
{
    private String promptText;
    private String answerText;
    private String intent;
    private String action;
    private Exception exception;
    private final Map<String, String> slots = Maps.newHashMap();



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
     * @return the intent found in the prompt
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


    /**
     * Sets a slot value
     * @param name slot key
     * @param value slot value
     * @return this instance
     */
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

    /**
     * Checks whether all slots in the answer have a value
     * @return whether all slots in the answer have a value
     */
    public boolean allSlotsSet()
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


    /**
     * Creates a ChatbotResponse containing a RuntimeException
     * @param message the error message to set in the exception
     * @return the created ChatbotResponse
     */
    public static ChatbotResponse forError(String message)
    {
        return new ChatbotResponse().setException(new RuntimeException(message));
    }



    /**
     * Creates a ChatbotResponse containing an exception
     * @param e the exception to set in the message
     * @return the created ChatbotResponse
     */
    public static ChatbotResponse forError(Exception e)
    {
        return new ChatbotResponse().setException(e);
    }

    @NonNull
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


    /* ------ Parcelable interface ------ */

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(promptText);
        parcel.writeString(answerText);
        parcel.writeString(intent);
        parcel.writeString(action);
        parcel.writeInt(slots.size());
        for (Map.Entry<String, String> e : slots.entrySet())
        {
            parcel.writeString(e.getKey());
            parcel.writeString(e.getValue());
        }
        int hasException = (exception == null)? 0 : 1;
        parcel.writeInt(hasException);
        if (hasException > 0)
            parcel.writeSerializable(exception);
    }

    public static final Parcelable.Creator<ChatbotResponse> CREATOR = new Parcelable.Creator<ChatbotResponse>()
    {

        @Override
        public ChatbotResponse createFromParcel(Parcel in) {
            ChatbotResponse response = new ChatbotResponse();
            response.setPromptText(in.readString())
                    .setAnswerText(in.readString())
                    .setIntent(in.readString())
                    .setAction(in.readString());
            int numSlots = in.readInt();
            for (int i = 0; i < numSlots; i++)
            {
                String key = in.readString();
                String value = in.readString();
                response.setSlot(key, value);
            }

            int hasException = in.readInt();
            if (hasException > 0)
                response.setException((Exception) in.readSerializable());
            return response;
        }

        @Override
        public ChatbotResponse[] newArray(int size) {
            return new ChatbotResponse[size];
        }
    };
}
