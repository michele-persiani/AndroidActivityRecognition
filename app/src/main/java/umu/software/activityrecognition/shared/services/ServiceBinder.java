package umu.software.activityrecognition.shared.services;

import android.app.Service;
import android.os.Binder;

/**
 * Helper class of a binder that holds a reference to its service
 * @param <S> class of the Service
 */
public class ServiceBinder<S extends Service> extends Binder
{
    private final S mService;

    public ServiceBinder(S service)
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
