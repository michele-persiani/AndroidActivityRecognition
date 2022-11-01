package umu.software.activityrecognition.shared.lifecycles;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.api.client.util.Lists;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import umu.software.activityrecognition.shared.util.AndroidUtils;


/**
 * Lifecycle that registers listens for network capabilities
 */
public class NetworkCallbackLifecycle extends ConnectivityManager.NetworkCallback implements DefaultLifecycleObserver
{
    private final Context mContext;
    private final int[] mCapabilities;
    private int mAvailability = 0;

    private final List<Network> mAvailableNetworks = Lists.newArrayList();

    private final List<Runnable> mCommands = Lists.newArrayList();

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    /**
     *
     * @param context the calling context
     * @param capabilities See  NetworkCapabilities
     */
    public NetworkCallbackLifecycle(Context context, int... capabilities)
    {
        mContext = context.getApplicationContext();
        mCapabilities = capabilities;
    }

    public int[] getCapabilities()
    {
        return mCapabilities;
    }

    /**
     * Enqueue a command to be executed when the network is available. That is either now or later
     * @param command command to enqueue
     */
    public synchronized void enqueueCommand(Runnable command)
    {
        if (isNetworkAvailable())
            mExecutor.submit(command);
        else
            mCommands.add(command);
    }


    /**
     * Clears the command queue
     */
    public synchronized void clearCommandsQueue()
    {
        mCommands.clear();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
        ConnectivityManager manager = AndroidUtils.getConnectivityManager(mContext);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        for (int c : mCapabilities)
            builder.addCapability(c);
        manager.registerNetworkCallback(builder.build(), this);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
        ConnectivityManager manager = AndroidUtils.getConnectivityManager(mContext);
        manager.unregisterNetworkCallback(this);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        clearCommandsQueue();
        mExecutor.shutdownNow();
    }

    @Override
    public synchronized void onAvailable(@NonNull Network network)
    {
        super.onAvailable(network);
        mAvailability += 1;
        mAvailableNetworks.add(network);

        while (mCommands.size() > 0)
            mExecutor.submit(mCommands.remove(0));
    }

    @Override
    public synchronized void onLost(@NonNull Network network)
    {
        super.onLost(network);
        mAvailability = Math.max(0, mAvailability - 1);
        mAvailableNetworks.remove(network);
    }

    /**
     * Returns the first available network, or null
     * @return the first available network, or null
     */
    @Nullable
    public Network getNetwork()
    {
        return (mAvailableNetworks.size() > 0)? mAvailableNetworks.get(0) : null;
    }

    /**
     * Returns whether there is at least a network available
     * @return whether there is at least a network available
     */
    public boolean isNetworkAvailable()
    {
        return mAvailability > 0;
    }



    /**
     * Instance for networks with internet (eg. cellular, wifi, bluetooth)
     * @param context the calling context
     * @return a new instance of NetworkCallbackLifecycle registering for the required capabilities
     */
    public static NetworkCallbackLifecycle withInternet(Context context)
    {
        return new NetworkCallbackLifecycle(context,
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        );
    }

    /**
     * Instance for wifi networks with internet
     * @param context the calling context
     * @return a new instance of NetworkCallbackLifecycle registering for the required capabilities
     */
    public static NetworkCallbackLifecycle withInternetWifi(Context context)
    {
        return new NetworkCallbackLifecycle(context,
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
                NetworkCapabilities.TRANSPORT_WIFI
        );
    }
}
