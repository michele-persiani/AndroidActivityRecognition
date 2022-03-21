package umu.software.activityrecognition.common;

import android.content.Context;

/**
 * Object that ties to the lifecycle of an Android component, using it a context
 */
public interface ComponentLifecycle
{
    void onCreate(Context context);

    void onStart(Context context);

    void onStop(Context context);

    void onDestroy(Context context);
}
