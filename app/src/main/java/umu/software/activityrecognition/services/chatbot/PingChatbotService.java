package umu.software.activityrecognition.services.chatbot;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.startup.AppInitializer;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import umu.software.activityrecognition.preferences.PingEventServicePreferences;
import umu.software.activityrecognition.preferences.initializers.PingEventPreferencesInitializer;
import umu.software.activityrecognition.shared.services.ServiceBinder;
import umu.software.activityrecognition.shared.util.RepeatingBroadcast;
import umu.software.activityrecognition.shared.services.LifecycleService;
import umu.software.activityrecognition.shared.preferences.Preference;


/**
 * Service to ping the DialogflowService with a predefined event (set through PingEventServicePreferences).
 * Pinging is performed through broadcasts
 * Uses the preference module PingEventPreferences to store event name, ping frequency, and whether it should be enabled
 * Broadcasts will have action 'DialogflowService.ACTION_SEND_EVENT'
 *
 * Available started actions:
 *  - ACTION_START
 *  - ACTION_STOP
 */
public class PingChatbotService extends LifecycleService
{
    public static final String ACTION_START             = "umu.software.activityrecognition.ACTION_START";
    public static final String ACTION_STOP              = "umu.software.activityrecognition.ACTION_STOP";


    private RepeatingBroadcast mBroadcast;
    private PingEventServicePreferences mPreferences;
    private Consumer<Preference<Integer>> mPrefCallback;


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return new ServiceBinder<>(this);
    }

    public boolean isSendingPingEvents()
    {
        return mBroadcast.isBroadcasting();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mPreferences = AppInitializer
                .getInstance(this)
                .initializeComponent(PingEventPreferencesInitializer.class);
        mBroadcast = new RepeatingBroadcast(this);

        mPreferences.pingEventMinutes().registerListener(p -> startBroadcasting());
        mPreferences.sendPingEvent().registerListener(p -> {
            if (!p.get())
                stopAndQuit();
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        switch (action)
        {
            case ACTION_START:
                startBroadcasting();
                break;
            case ACTION_STOP:
            default:
                stopAndQuit();
                break;
        }

        if (!mPreferences.sendPingEvent().get(false))
            stopAndQuit();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mBroadcast.stop();
        mPreferences.clearListeners();
    }

    protected void startBroadcasting()
    {
        if (!mPreferences.sendPingEvent().get(false))
            return;

        Intent dialogflowIntent = createDialogflowIntent();
        int pingMinutes = mPreferences.pingEventMinutes().get();


        mBroadcast.stop();
        mBroadcast.start(
                TimeUnit.MILLISECONDS.convert(pingMinutes, TimeUnit.MINUTES),
                ((context, intent) -> {
                    startService(dialogflowIntent);
                    logger().i("sendChatbotEvent() -> %s", dialogflowIntent);
                })
        );
        logger().i("Sending event (%s) every (%s) minutes", mPreferences.pingEventName().get(), pingMinutes);
    }


    public boolean isBroadcasting()
    {
        return mBroadcast.isBroadcasting();
    }


    private void stopAndQuit()
    {
        logger().i("Stopping sending events to %s", DialogflowService.class.getSimpleName());
        stopSelf();
    }

    /**
     * Methods that creates the broadcasted intent
     * @return intent to broadcast
     */
    private Intent createDialogflowIntent()
    {
        Intent intent = new Intent(this, DialogflowService.class);
        intent.setAction(DialogflowService.ACTION_SEND_EVENT);
        intent.putExtra(DialogflowService.EXTRA_EVENT_NAME, mPreferences.pingEventName().get());
        return intent;
    }

    /**
     * Helper method to start pinging DialogflowService
     * @param context calling context
     */
    public static void startPingDialogflowService(Context context)
    {
        Intent pingIntent = new Intent(context, PingChatbotService.class);
        pingIntent.setAction(PingChatbotService.ACTION_START);
        context.startService(pingIntent);
    }

    /**
     * Helper method to stop pinging DialogflowService
     * @param context calling context
     */
    public static void stopPingDialogflowService(Context context)
    {
        Intent pingIntent = new Intent(context, PingChatbotService.class);
        pingIntent.setAction(PingChatbotService.ACTION_STOP);
        context.startService(pingIntent);
    }

}
