package umu.software.activityrecognition.shared.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

import umu.software.activityrecognition.shared.util.ParcelableUtil;



/**
 * Service to broadcast intents between wearable nodes.
 * Intents should be small in size
 *
 * Only intents passing the configured intnet filters are forwarded to the WearOS network, and all intents
 * coming from the WearOS network are broadcasted locally
 *
 * Actions:
 *  - ACTION_ADD_BROADCAST
 *      Adds an IntentFilter that specifies which broadcasts should be sent over the WearOS nodes
 *  - ACTION_REMOVE_BROADCAST
 *      Removes a previously added IntentFilter
 *
 */
public class WearIntentBridgeService extends Service implements CapabilityClient.OnCapabilityChangedListener, MessageClient.OnMessageReceivedListener
{
    public static final String ACTION_ADD_BROADCAST = "umu.software.activityrecognition.ACTION_ADD_BROADCAST";
    public static final String ACTION_REMOVE_BROADCAST = "umu.software.activityrecognition.ACTION_REMOVE_BROADCAST";
    public static final String EXTRA_INTENT_FILTER = "EXTRA_INTENT_FILTER";


    public static final String BROADCAST_BRIDGE_CAPABILITY = "umu.software.activityrecognition.BROADCAST_BRIDGE_CAPABILITY";
    protected static final String MESSAGE_PATH_MAIN = "main/";

    private CapabilityClient mCapabilityClient;

    private Set<Node> mNodes = Sets.newHashSet();
    private MessageClient mMessageClient;
    private final Map<IntentFilter, BroadcastReceiver> mBroadcastReceivers = Maps.newHashMap();


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mCapabilityClient = Wearable.getCapabilityClient(this);
        mMessageClient = Wearable.getMessageClient(this);

        mCapabilityClient.addLocalCapability(BROADCAST_BRIDGE_CAPABILITY);
        mCapabilityClient.addListener(this, BROADCAST_BRIDGE_CAPABILITY);
        mMessageClient.addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        if (intent == null)
            return START_STICKY;

        switch (intent.getAction())
        {
            case ACTION_ADD_BROADCAST:
                onAddBroadcast(intent);
                break;
            case ACTION_REMOVE_BROADCAST:
                onRemoveBroadcast(intent);
                break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mCapabilityClient.removeLocalCapability(BROADCAST_BRIDGE_CAPABILITY);
        mCapabilityClient.removeListener(this);
        mMessageClient.removeListener(this);
        for (BroadcastReceiver rec : mBroadcastReceivers.values())
            LocalBroadcastManager.getInstance(this).unregisterReceiver(rec);
    }


    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo)
    {
        mNodes = Sets.newHashSet(capabilityInfo.getNodes());
    }


    private void onAddBroadcast(Intent intent)
    {
        if (!intent.hasExtra(EXTRA_INTENT_FILTER)) return;

        IntentFilter filter = intent.getParcelableExtra(EXTRA_INTENT_FILTER);

        BroadcastReceiver br = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                broadcastIntentToNodes(intent);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(br, filter);
        mBroadcastReceivers.put(filter, br);
    }


    private void onRemoveBroadcast(Intent intent)
    {
        if (!intent.hasExtra(EXTRA_INTENT_FILTER)) return;

        IntentFilter filter = intent.getParcelableExtra(EXTRA_INTENT_FILTER);
        BroadcastReceiver br = mBroadcastReceivers.remove(filter);
        if (br != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
    }


    private void broadcastIntentToNodes(Intent intent)
    {
        byte[] bytes = ParcelableUtil.marshall(intent);
        for (Node n : mNodes)
            mMessageClient.sendMessage(n.getId(), MESSAGE_PATH_MAIN, bytes);
    }


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent)
    {
        byte[] bytes = messageEvent.getData();
        Intent intent = ParcelableUtil.unmarshall(bytes, Intent.CREATOR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
