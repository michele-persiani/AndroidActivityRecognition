package umu.software.activityrecognition.shared.resourceaccess;


import android.util.Log;

import java.util.Arrays;



/**
 * Token to request access to exclusive resources.
 * Tokens are instantiated through ExclusiveResourceAccess.getInstance().newToken()
 */
public class ExclusiveAccessToken implements Comparable<ExclusiveAccessToken>
{
        private static int n = 0;

        private final int myN;

        private final ExclusiveResourceAccess accessManager;
        private final int priority;
        private final String[] resources;

        private Runnable higherPriorityAcquisitionCallback;
        private Runnable resourceAcquiredCallback;
        private Runnable resourceReleasedCallback;


        protected ExclusiveAccessToken(ExclusiveResourceAccess accessManager, int priority, String... resources)
        {
                this.accessManager = accessManager;
                this.priority = priority;
                this.resources = resources;
                n += 1;
                myN = n;
        }

        @Override
        public String toString()
        {
                return "ExclusiveAccessToken{" +
                        "n=" + myN +
                        "priority=" + priority +
                        '}';
        }

        public int priority()
        {
                return priority;
        }


        public boolean isAcquired()
        {
                return accessManager.hasAcquiredAllResources(this);
        }

        public boolean acquire(boolean await)
        {
                return accessManager.requestResources(this, await);
        }

        /**
         * Attempts to acquire the token and run the given command. After execution the token is released
         * if it was acquired internally to this function
         * @param command
         * @param await
         */
        public boolean acquireAndRun(Runnable command, boolean await)
        {
                boolean alreadyAcquired = isAcquired();
                boolean acquired = true;
                boolean result = false;

                if (!alreadyAcquired)
                        acquired = acquire(await);

                if (alreadyAcquired || acquired)
                {
                        command.run();
                        result = true;
                }

                if (!alreadyAcquired && acquired)
                        release();
                return result;
        }


        public boolean release()
        {
                return accessManager.releaseResource(this);
        }


        public String[] resources()
        {
                return resources;
        }


        public void onHigherPriorityAcquisition()
        {
                if(higherPriorityAcquisitionCallback != null)
                        higherPriorityAcquisitionCallback.run();
        }


        public void onResourcesAcquired()
        {
                Log.i(getClass().getSimpleName(), String.format("Token-%s acquired exclusive resources %s", myN, Arrays.toString(resources)));
                if(resourceAcquiredCallback != null)
                        resourceAcquiredCallback.run();
        }


        public void onResourcesReleased()
        {
                Log.i(getClass().getSimpleName(), String.format("Token-%s released exclusive resources %s", myN, Arrays.toString(resources)));
                if(resourceReleasedCallback != null)
                        resourceReleasedCallback.run();
        }


        @Override
        public int compareTo(ExclusiveAccessToken exclusiveAccessToken)
        {
                return exclusiveAccessToken.priority() - priority();
        }


        /**
         * Sets a callback called whenever a token gets its resource acquired by a higher priority token
         * @param callback callback to set
         */
        public void setHigherPriorityAcquisitionCallback(Runnable callback)
        {
                higherPriorityAcquisitionCallback = callback;
        }


        /**
         * Sets a callback called whenever a token acquire its resources either right after request or because all
         * higher priority tokens have released theirs
         * @param callback callback to set
         */
        public void setResourceAcquiredCallback(Runnable callback)
        {
                resourceAcquiredCallback = callback;
        }

        /**
         * Sets a callback called whenever a token gets its resource released also when they are acquired
         * by a higher priority token
         * @param callback callback to set
         */
        public void setResourceReleasedCallback(Runnable callback)
        {
                resourceReleasedCallback = callback;
        }
}
