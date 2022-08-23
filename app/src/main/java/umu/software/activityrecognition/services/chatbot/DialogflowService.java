package umu.software.activityrecognition.services.chatbot;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.startup.AppInitializer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.chatbot.Chatbot;
import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.chatbot.ChatbotResponseCallback;
import umu.software.activityrecognition.chatbot.DialogflowChatbot;
import umu.software.activityrecognition.chatbot.SpeechChatbot;
import umu.software.activityrecognition.preferences.DialogflowServicePreferences;
import umu.software.activityrecognition.preferences.initializers.ChatbotPreferencesInitializer;
import umu.software.activityrecognition.services.LocalBinder;
import umu.software.activityrecognition.shared.preferences.Preference;
import umu.software.activityrecognition.shared.util.UniqueId;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.lifecycles.LifecycleService;


/**
 * Started+Bound service to access a Dialogflow chatbot.
 *
 * Available started actions:
 * - ACTION_CONFIGURE
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
 * removeResponseCallback(), clearResponseCallback()
 *
 * setResponseCallback() allows to specify callbacks for the incoming message in the form of BroadcastReceivers.
 * These receivers are stored (ie referenced) by the service so can continue to run also after their sender has
 * terminated
 *
 *
 * Requires Google Cloud API Key to function. These can be sent with starting or binding intents, otherwise
 * the default API key from the preferences will be used
 *
 */
public class DialogflowService extends LifecycleService
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


    /** Configure the service. Nb. extras are optional and the behavior of configure
     * depends on the provided extras */
    public static final String ACTION_CONFIGURE             = "umu.software.activityrecognition.ACTION_CONFIGURE";
    /** Whether this should be configured as a foreground service showing a notification to the user: Bool */
    public static final String EXTRA_FOREGROUND             = "EXTRA_FOREGROUND";


    /** Shutdown the chatbot. Doesn't have effect if the service is being bound by another component*/
    public static final String ACTION_SHUTDOWN              = "umu.software.activityrecognition.ACTION_SHUTDOWN";

    /** Action used to identify broadcasted chatbot responses.
     * See setResponseCallback()
     * */
    public static final String ACTION_CHATBOT_RESPONSE = "umu.software.activityrecognition.ACTION_HANDLE_RESPONSE";
    /** Chatbot response: ChatbotResponse (Parcelable) */
    public static final String EXTRA_RESPONSE               = "EXTRA_RESPONSE";


    private SpeechChatbot mChatBot;
    private LocalBinder<DialogflowService> mBinder;
    private DialogflowServicePreferences mPreferences;
    private ForegroundServiceLifecycle mForegroundLifecycle;

    private final Map<String, BroadcastReceiver> mResponsesCallbacks = Maps.newConcurrentMap();

    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = AppInitializer
                .getInstance(this)
                .initializeComponent(ChatbotPreferencesInitializer.class);
        connect();
        setVoicePersona();

        mPreferences.language().registerListener(p -> setVoicePersona());
        mPreferences.voiceSpeed().registerListener(p -> setVoicePersona());
        mPreferences.voiceName().registerListener(p -> setVoicePersona());
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
            mBinder = new LocalBinder<>(this);
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
            case ACTION_CONFIGURE:
                setForeground(extras);
                break;
            case ACTION_SEND_EVENT:
                sendChatbotEvent(extras);
                break;
            case ACTION_START_LISTENING:
                startListening();
                break;
            case ACTION_SHUTDOWN:
                stopForeground(true);
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
        mPreferences.clearListeners();
    }



    /**
     * Connect to the chatbot.
     */
    private void connect()
    {
        logger().i("Chatbot connecting...");

        if (mChatBot != null && mChatBot.isConnected())
            return;

        if (mChatBot != null)
            mChatBot.disconnect(r -> {});

        String jsonKey = mPreferences.apiKey().get();

        Chatbot chatbot = new DialogflowChatbot(jsonKey);
        chatbot = new ChatbotResponseCallback(chatbot, response -> {
            Intent broadcast = new Intent();
            broadcast.setAction(ACTION_CHATBOT_RESPONSE)
                    .putExtra(EXTRA_RESPONSE, (Parcelable) response);
            sendBroadcast(broadcast);
        });
        mChatBot = new SpeechChatbot(chatbot, true);

        mChatBot.connect(result -> logger().i("Chatbot connected (%s).", result));
    }



    /**
     * Show the foreground service notification
     * @param params bundle of parameters
     */
    @SuppressLint("LaunchActivityFromNotification")
    private void setForeground(Bundle params)
    {
        boolean startForeground = params.getBoolean(EXTRA_FOREGROUND, false);

        if (startForeground && !isForeground())
        {
            PendingIntent listenPendingIntent = PendingIntent.getService(this,10432,
                    new Intent(this, DialogflowService.class).setAction(ACTION_START_LISTENING),
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            PendingIntent stopPendingIntent = PendingIntent.getService(this,10433,
                    new Intent(this, DialogflowService.class).setAction(ACTION_SHUTDOWN),
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Action closeAction = new NotificationCompat.Action.Builder(null, getString(R.string.close), stopPendingIntent).build();

            mForegroundLifecycle = new ForegroundServiceLifecycle(
                    this,
                    UniqueId.uniqueInt(),
                    getString(R.string.notification_channel_id),
                    builder -> {
                        builder.setContentTitle(getString(R.string.chatbot_notification_title))
                                .setContentText(getString(R.string.chatbot_notification_text))
                                .setGroup(getString(R.string.notification_group_id))
                                .addAction(closeAction)
                                .setSmallIcon(R.mipmap.ic_watch_round)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                                .setContentIntent(listenPendingIntent)
                                .setAutoCancel(false)
                                .setOngoing(true);
                    }
            );

            getLifecycle().addObserver(mForegroundLifecycle);
        }
        else if(!startForeground && isForeground())
        {
            getLifecycle().removeObserver(mForegroundLifecycle);
            mForegroundLifecycle = null;
            stopForeground(true);
        }
    }

    /**
     * Sets the voice language and parameters of the chatbot
     */
    private void setVoicePersona()
    {
        String language = mPreferences.language().get();
        float voiceSpeed = mPreferences.voiceSpeed().get()/100.f;
        String voiceName = mPreferences.voiceName().get();

        Function<List<Voice>, Integer> voiceSelector = voices -> {
            List<String> names = voices.stream().map(Voice::getName).collect(Collectors.toList());
            if (names.contains(voiceName))
                return names.indexOf(voiceName);
            return names.contains(voiceName) ? names.indexOf(voiceName) : 0;
        };

        applyIfConnected(speechChatbot -> {
            speechChatbot.setVoicePersona(
                    Locale.forLanguageTag(language),
                    voiceSpeed,
                    null                                        // TODO voices for languages different from the current locale don't work
            );
            logger().i("Set voice persona with language (%s), speed (%s) and voice (%s)",
                    language,
                    voiceSpeed,
                    voiceName
            );
        });
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
     * Returns the available voices for the current persona
     * @return available voices
     */
    public List<Voice> getAvailableVoices()
    {
        return applyIfConnected(SpeechChatbot::getAvailableVoices, Lists.newArrayList());
    }


    /**
     * Start listening with the chatbot
     */
    public void startListening()
    {
        applyIfConnected(chatbot -> chatbot.startListening(response -> {}));
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
                if (filter.test(response))
                    consumer.accept(context, response);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CHATBOT_RESPONSE);

        removeResponseCallback(callbackId);
        registerReceiver(receiver, intentFilter);
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
        unregisterReceiver(receiver);
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
     * @return whether the service is in a foreground state
     */
    public boolean isForeground()
    {
        return mForegroundLifecycle != null;
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
