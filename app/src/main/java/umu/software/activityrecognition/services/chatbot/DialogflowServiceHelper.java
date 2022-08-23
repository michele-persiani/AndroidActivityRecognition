package umu.software.activityrecognition.services.chatbot;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.api.client.util.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import umu.software.activityrecognition.services.LocalBinder;
import umu.software.activityrecognition.services.ServiceConnectionHandler;


/**
 * Helper class to utilize the DialogflowService
 */
public class DialogflowServiceHelper
{
    private final Context mContext;

    private DialogflowServiceHelper(Context context)
    {
        mContext = context.getApplicationContext();
    }


    public static DialogflowServiceHelper newInstance(Context context)
    {
        return new DialogflowServiceHelper(context);
    }


    public ServiceConnectionHandler<LocalBinder<DialogflowService>> bind()
    {
        return new ServiceConnectionHandler<LocalBinder<DialogflowService>>(mContext)
                .setAutoRebind(true)
                .bind(DialogflowService.class);
    }


    public void configure(@Nullable Boolean foreground)
    {
        Intent intent = newIntent(DialogflowService.ACTION_CONFIGURE);
        if (foreground != null) intent.putExtra(DialogflowService.EXTRA_FOREGROUND, foreground);
        mContext.startService(intent);
    }

    public void shutdownChatbot()
    {
        Intent intent = newIntent(DialogflowService.ACTION_SHUTDOWN);
        mContext.startService(intent);
    }

    public void startListening()
    {
        Intent intent = newIntent(DialogflowService.ACTION_START_LISTENING);
        mContext.startService(intent);
    }


    public void sendChatbotEvent(String eventName)
    {
        Intent intent = newIntent(DialogflowService.ACTION_SEND_EVENT);
        intent.putExtra(DialogflowService.EXTRA_EVENT_NAME, eventName);
        mContext.startService(intent);
    }


    public void sendChatbotEvent(String eventName, Map<String, String> slots)
    {
        Intent intent = newIntent(DialogflowService.ACTION_SEND_EVENT);
        intent.putExtra(DialogflowService.EXTRA_EVENT_NAME, eventName);
        ArrayList<String> slotsNames = Lists.newArrayList();
        ArrayList<String> slotsValues = Lists.newArrayList();

        for (Map.Entry<String, String> e : slots.entrySet())
        {
            slotsNames.add(e.getKey());
            slotsValues.add(e.getValue());
        }
        intent.putStringArrayListExtra(DialogflowService.EXTRA_SLOTS_NAMES, slotsNames);
        intent.putStringArrayListExtra(DialogflowService.EXTRA_SLOTS_VALUES, slotsValues);
        mContext.startService(intent);
    }


    private Intent newIntent(String action)
    {
        return new Intent(mContext, DialogflowService.class).setAction(action);
    }
}
