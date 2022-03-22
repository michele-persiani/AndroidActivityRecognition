package umu.software.activityrecognition.common.lifecycles;

import android.content.Context;

/**
 * Object that ties to the lifecycle of an Android component, using it a context
 */
public interface LifecycleElement
{
    public enum LifecycleState
    {
        DESTROYED,
        CREATED,
        STARTED,
        STOPPED
    }

    void onCreate(Context context);

    void onStart(Context context);

    void onStop(Context context);

    void onDestroy(Context context);
}
