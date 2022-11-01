package umu.software.activityrecognition.services.chatbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.Wearable;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.chatbot.ChatbotCallbackWrapper;
import umu.software.activityrecognition.chatbot.impl.SpeechChatbot;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.services.ServiceBinder;


/**
 * Started+Bound service to access a chatbot.
 * NB: you must set a chatbot through setup() !!
 *
 * Available started actions:
 * - ACTION_SEND_EVENT
 * - ACTION_START_LISTENING
 * - ACTION_SHUTDOWN
 *
 * See static variables for the extras to set for each action
 *
 * Broadcasts of chatbot messages: Receivers should register with filter of action=ACTION_HANDLE_RESPONSE,
 * In every broadcast the corresponding ChatbotResponse can be found in the intent's extras
 * as Parcelable with key EXTRA_RESPONSE
 *
 * Functions available after binding:
 * startListening(), getAvailableVoices(), setVoice(), sendEvent(), setResponseCallback(),
 * removeResponseCallback(), clearResponseCallback(), setChatbot()
 *
 *
 * setResponseCallback() allows to specify callbacks for the incoming message in the form of BroadcastReceivers.
 * These receivers are stored (ie referenced) by the service so can continue to run also after their sender has
 * terminated
 *
 *
 *
 */
public class ChatbotService extends LifecycleService
{
    /** Send an event */
    public static final String ACTION_SEND_EVENT            = "umu.software.activityrecognition.ACTION_SEND_EVENT";
    public static final String EXTRA_EVENT_NAME             = "EXTRA_EVENT_NAME";
    /** Ordered list of slot names: List */
    public static final String EXTRA_SLOTS_NAMES            = "EXTRA_SLOTS_NAMES";
    /** Ordered list of slot values: List */
    public static final String EXTRA_SLOTS_VALUES           = "EXTRA_SLOTS_VALUES";

    /** Start listening */
    public static final String ACTION_START_LISTENING       = "umu.software.activityrecognition.ACTION_START_LISTENING";


    /** Shutdown the chatbot. Doesn't have effect if the service is being bound by another component*/
    public static final String ACTION_SHUTDOWN              = "umu.software.activityrecognition.ACTION_SHUTDOWN";

    /** Action used to identify broadcasted chatbot responses.
     * See setResponseCallback()
     * */
    public static final String ACTION_CHATBOT_RESPONSE = "umu.software.activityrecognition.ACTION_HANDLE_RESPONSE";
    /** Chatbot response: ChatbotResponse (Parcelable) */
    public static final String EXTRA_RESPONSE               = "EXTRA_RESPONSE";


    private SpeechChatbot mChatBot;
    private ServiceBinder<ChatbotService> mBinder;

    private final Map<String, BroadcastReceiver> mResponsesCallbacks = Maps.newConcurrentMap();

    @Override
    public void onCreate()
    {
        super.onCreate();
    }


    /**
     * The Google API Key can be set in the binding intent as an extra string EXTRA_DIALOGFLOW_KEY.
     * In this way the chatbot will be initialized the first time the service is bound
     * @param intent binding intent
     * @return the service's binder
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        logger().i("onBind() -> %s", intent);
        if (mBinder == null)
            mBinder = new ServiceBinder<>(this);
        return mBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        Bundle extras = (intent.getExtras() != null)? intent.getExtras() : new Bundle();
        switch (action)
        {
            case ACTION_SEND_EVENT:
                sendChatbotEvent(extras);
                break;
            case ACTION_START_LISTENING:
                startListening();
                break;
            case ACTION_SHUTDOWN:
                stopSelf();
                break;
            default:
        }
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        int cleared = clearResponseCallbacks();
        logger().i("Cleared (%s) broadcast receivers", cleared);
        applyIfConnected(chatbot -> {
            chatbot.disconnect(result -> {
                logger().i("Chatbot disconnected (%s)", result);
            });
        });
    }



    /**
     * Setup this service to use a specific chatbot
     * @param chatbot the chatbot that this service will interact with
     */
    public void setup(Chatbot chatbot)
    {
        logger().i("Initializing chatbot...");

        if (mChatBot != null && mChatBot.isConnected())
            mChatBot.disconnect(r -> {});

        chatbot = new ChatbotCallbackWrapper(chatbot, response -> {
            Intent broadcast = new Intent();
            broadcast.setAction(ACTION_CHATBOT_RESPONSE)
                    .putExtra(EXTRA_RESPONSE, (Parcelable) response);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
        });
        mChatBot = new SpeechChatbot(this, chatbot);

        mChatBot.connect(result -> logger().i("Chatbot connected (%s).", result));

    }




    /**
     * Send an event to the chatbot
     * @param params bundle of parameters. Uses EXTRA_EVENT_NAME
     */
    private void sendChatbotEvent(@NonNull Bundle params)
    {
        if (!params.containsKey(EXTRA_EVENT_NAME))
            return;
        String eventName = params.getString(EXTRA_EVENT_NAME);


        Map<String, String> slots = null;
        if (params.containsKey(EXTRA_SLOTS_NAMES) && params.containsKey(EXTRA_SLOTS_VALUES))
        {
            List<String> names = params.getStringArrayList(EXTRA_SLOTS_NAMES);
            List<String> values = params.getStringArrayList(EXTRA_SLOTS_VALUES);
            if (names.size() == values.size())
            {
                slots = Maps.newHashMap();
                for (int i = 0; i < names.size(); i ++)
                    slots.put(names.get(i), values.get(i));
            }
            else
                logger().w("Sent event (%s) with (%s) slots names and (%s) slots values",
                        eventName,
                        names,
                        values
                );
        }

        sendChatbotEvent(eventName, slots);
    }



    /**
     * Send an event to the chatbot
     * @param eventName the event's name
     * @param eventArgs the event's parameters
     */
    public void sendChatbotEvent(String eventName, @Nullable Map<String, String> eventArgs)
    {
        Map<String, String> args = (eventArgs == null)? Maps.newHashMap() : eventArgs;
        applyIfConnected(chatbot -> chatbot.sendEvent(eventName, args, response -> {}));
    }



    /**
     * Start listening with the chatbot
     */
    public void startListening()
    {
        applyIfConnected(chatbot -> chatbot.startListening(response -> {}));
    }


    /**
     * Sets a callback for the actions sent by the chatbot. Overrides the previously given
     * callback for the specified key
     * @param callbackId id to access again the callback
     * @param filter filter to specify which responses are to be handled
     * @param consumer the callback function to register, or null to remove the previously added callback
     */
    public void setResponseCallback(String callbackId, Predicate<ChatbotResponse> filter, BiConsumer<Context, ChatbotResponse> consumer)
    {
        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                ChatbotResponse response = intent.getParcelableExtra(EXTRA_RESPONSE);
                if (response != null && filter.test(response))
                    consumer.accept(context, response);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CHATBOT_RESPONSE);

        removeResponseCallback(callbackId);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        mResponsesCallbacks.put(callbackId, receiver);
    }


    /**
     * Removes a callback  previously added through setResponseCallback()
     * @param callbackId id of the callback to remove
     * @return whether a callback with the given key was found and removed
     */
    public boolean removeResponseCallback(String callbackId)
    {
        if (!mResponsesCallbacks.containsKey(callbackId))
            return false;
        BroadcastReceiver receiver = mResponsesCallbacks.remove(callbackId);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        return true;
    }


    /**
     * Clears all callbacks previously added through setResponseCallback()
     */
    public int clearResponseCallbacks()
    {
        int cleared = 0;
        for (String r : mResponsesCallbacks.keySet())
            cleared += removeResponseCallback(r)? 1 : 0;
        return cleared;
    }


    /** Returns whether the chatbot is initialized and connected
     * @return Whether the chatbot is initialized and connected
     */
    public boolean isConnected()
    {
        return mChatBot != null && mChatBot.isConnected();
    }




    /**
     * Apply an operation to the chatbot only if it is connected
     * @param operation the operation to execute
     * @return whether the operation was executed
     */
    public boolean applyIfConnected(Consumer<SpeechChatbot> operation)
    {
        if (isConnected())
        {
            operation.accept(mChatBot);
            return true;
        }
        return false;
    }


    /**
     * Apply an operation to the chatbot
     * @param operation the operation to execute
     * @param defaultValue the default return value of the operation
     * @return the operation's result or the default value if it couldn't execute
     */
    public <R> R applyIfConnected(Function<SpeechChatbot, R> operation, R defaultValue)
    {
        if (isConnected())
            return operation.apply(mChatBot);
        return defaultValue;
    }

}
