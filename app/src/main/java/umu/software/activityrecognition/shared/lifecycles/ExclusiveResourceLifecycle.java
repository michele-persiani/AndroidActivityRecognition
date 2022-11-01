package umu.software.activityrecognition.shared.lifecycles;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import org.jetbrains.annotations.NotNull;
import umu.software.activityrecognition.shared.resourceaccess.ExclusiveAccessToken;
import umu.software.activityrecognition.shared.resourceaccess.ExclusiveResourceAccess;

import java.util.HashMap;
import java.util.Map;



/**
 * Lifecycle to manage a set of exclusive resource tokens
 */
public class ExclusiveResourceLifecycle implements DefaultLifecycleObserver
{


    private final Map<Object, ExclusiveAccessToken> mTokensMap = new HashMap<>();


    /**
     * Register an exclusive token that can later be retrieved through getToken()
     * @param key key identifying the token
     * @param priority token's priority when accessing the exclusive resources
     * @param resources requested exclusive resources
     */
    public void registerToken(Object key, int priority, String... resources)
    {
        mTokensMap.put(key, ExclusiveResourceAccess.getInstance().createAccessToken(priority, resources));
    }

    /**
     * Checks whether a token was previously inserted using registerToken()
     * @param key token's key
     * @return whether the token was previously inserted
     */
    public boolean hasToken(Object key)
    {
        return mTokensMap.containsKey(key);
    }


    /**
     * Gets a previously inserted token
     * @param key token's key
     * @return the token
     */
    public ExclusiveAccessToken getToken(Object key)
    {
        return mTokensMap.get(key);
    }


    @Override
    public void onDestroy(@NonNull @NotNull LifecycleOwner owner)
    {
        DefaultLifecycleObserver.super.onDestroy(owner);
        mTokensMap.keySet().forEach( k -> mTokensMap.get(k).release());
    }

}
