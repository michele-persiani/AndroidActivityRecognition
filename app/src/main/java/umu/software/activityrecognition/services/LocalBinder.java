package umu.software.activityrecognition.services;

import android.app.Service;
import android.os.Binder;

/**
 * Helper class of a binder that encapsulates its service
 * @param <S> class of the Service
 */
public class LocalBinder<S extends Service> extends Binder
{
    private final S mService;

    public LocalBinder(S service)
    {
        mService = service;
    }

    /**
     * Get the service owning the binder
     * @return the service owning the binder
     */
    public S getService()
    {
        return mService;
    }
}
