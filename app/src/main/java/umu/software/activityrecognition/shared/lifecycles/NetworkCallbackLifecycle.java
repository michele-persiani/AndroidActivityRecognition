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

import umu.software.activityrecognition.shared.AndroidUtils;


public class NetworkCallbackLifecycle extends ConnectivityManager.NetworkCallback implements DefaultLifecycleObserver
{
    private final Context mContext;
    private final int[] mCapabilities;
    private int mAvailability = 0;

    private final List<Network> mAvailableNetworks = Lists.newArrayList();

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
    public void onAvailable(@NonNull Network network)
    {
        super.onAvailable(network);
        mAvailability += 1;
        mAvailableNetworks.add(network);
    }

    @Override
    public void onLost(@NonNull Network network)
    {
        super.onLost(network);
        mAvailability = Math.max(0, mAvailability - 1);
        mAvailableNetworks.remove(network);
    }

    @Nullable
    public Network getNetwork()
    {
        return (mAvailableNetworks.size() > 0)? mAvailableNetworks.get(0) : null;
    }

    public boolean isCapabilityAvailable()
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
