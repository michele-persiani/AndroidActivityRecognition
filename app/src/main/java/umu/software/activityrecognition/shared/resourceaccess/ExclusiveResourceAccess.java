package umu.software.activityrecognition.shared.resourceaccess;


import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;




/**
 * Class to control that a set of resources have exclusive access. Tokens can be requested to exclusively access a set of resources.
 * If a token with higher priority requests a utilized resource then the previous token is notified then it should release the resource
 */
public class ExclusiveResourceAccess
{

    private static ExclusiveResourceAccess sInstance;

    private final Queue<ExclusiveAccessToken> tokensAwaitingRelease = Queues.newPriorityQueue();
    private final Map<String, ExclusiveAccessToken> resourcesMap = Maps.newHashMap();


    public static ExclusiveResourceAccess getInstance()
    {
        if(sInstance == null)
            sInstance = new ExclusiveResourceAccess();
        return sInstance;
    }


    /**
     * Checks whether the given token have availability on its required resources
     * @param token token to check
     * @return  whether the given token have availability on its required resources
     */
    public boolean areResourcesAvailable(ExclusiveAccessToken token)
    {
        for(String r : token.resources())
            if (resourcesMap.containsKey(r) && resourcesMap.get(r) != token && token.compareTo(resourcesMap.get(r)) >= 0)
                return false;
        return true;
    }

    /**
     * Returns whether the given token possesses exclusive access on all of its required resources
     * @param token token to check
     * @return whether the given token possesses exclusive access on all of its required resources
     */
    public boolean hasAcquiredAllResources(ExclusiveAccessToken token)
    {
        for(String r : token.resources())
            if (!resourcesMap.containsKey(r) || (resourcesMap.containsKey(r) && resourcesMap.get(r) != token))
                return false;
        return true;
    }

    /**
     * Requests the resources required by the given token
     * @param token token requiring resources
     * @param await whether the token is to be put in the waiting link in the case the resources are unavailable
     * @return whether the token acquired the resources
     */
    protected boolean requestResources(ExclusiveAccessToken token, boolean await)
    {
        if (hasAcquiredAllResources(token))
            return true;

        if (!areResourcesAvailable(token))
        {
            if (await)
                tokensAwaitingRelease.add(token);
            return false;
        }

        Set<ExclusiveAccessToken> notifiedTokens = Sets.newHashSet();

        Arrays.stream(token.resources())
                .forEach( r -> {
                    ExclusiveAccessToken prevToken = resourcesMap.get(r);
                    if (resourcesMap.containsKey(r) && prevToken != null && !notifiedTokens.contains(prevToken))
                    {
                        prevToken.onHigherPriorityAcquisition();
                        releaseResource(prevToken);
                        notifiedTokens.add(prevToken);
                        tokensAwaitingRelease.add(prevToken);
                    }
                    resourcesMap.put(r, token);
                });
        tokensAwaitingRelease.remove(token);
        token.onResourcesAcquired();
        return true;
    }


    /**
     * Releases resources for a requesting token
     * @param token
     * @return whether the token had acquired its resources and are now released
     */
    protected boolean releaseResource(ExclusiveAccessToken token)
    {
        for (String r : token.resources())
            if (!(resourcesMap.get(r) == token))
                return false;

        for (String r : token.resources())
            resourcesMap.remove(r);


        token.onResourcesReleased();
        notifyWaitingTokens();
        return true;
    }


    private void notifyWaitingTokens()
    {
        AtomicBoolean changed = new AtomicBoolean(false);
        tokensAwaitingRelease
                .stream()
                .filter(this::areResourcesAvailable)
                .sorted()
                .findFirst()
                .ifPresent(tok -> {
                    requestResources(tok, true);
                    changed.set(true);
                });
        if (changed.get())
            notifyWaitingTokens();
    }


    /**
     * Creates a new token for exclusive access of resources.
     * @param priority priority of the token
     * @param resources requested resources ids
     * @return a newly created token
     */
    public ExclusiveAccessToken createAccessToken(int priority, String... resources)
    {
        return new ExclusiveAccessToken(this, priority, resources);
    }



    /**
     * Run a piece of code only if the provided resources are available with the given priority
     * @param r command to run
     * @param priority priority of the operation
     * @param resources requested resources ids
     * @return whether the resources were available and the command successfully executed
     */
    public boolean runIfResourcesAvailable(Runnable r, int priority, String... resources)
    {
        ExclusiveAccessToken token = createAccessToken(priority, resources);
        boolean acquisition = token.acquire(false);
        if (acquisition)
        {
            r.run();
            token.release();
        }
        return acquisition;
    }


    public boolean runIfResourcesAvailable(Runnable r, ExclusiveAccessToken token)
    {
        boolean acquisition = token.acquire(false);
        if (acquisition)
        {
            r.run();
            token.release();
        }
        return acquisition;
    }
}
