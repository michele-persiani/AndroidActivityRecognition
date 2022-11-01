package umu.software.activityrecognition.shared.lifecycles;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;
import java.util.function.Function;

import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;



/**
 * Lifecycle observer handling a ServiceConnectionHandler
 * @param <T> class of bound binder
 */
public class BinderLifecycle<T extends Binder> implements DefaultLifecycleObserver
{

    private final ServiceConnectionHandler<T> mConnection;
    private final Consumer<Intent> mIntentBuilder;
    Class<? extends Service> mServiceClass;


    /**
     *
     * @param context calling context
     * @param serviceClass service to bind
     * @param intentBuilder optional builder to create the binding intent
     */
    public BinderLifecycle(Context context, Class<? extends Service> serviceClass, @Nullable Consumer<Intent> intentBuilder)
    {
        mConnection = new ServiceConnectionHandler<>(context.getApplicationContext());
        mServiceClass = serviceClass;
        mIntentBuilder = intentBuilder;
    }




    @Override
    public void onStart(@NonNull LifecycleOwner owner)
    {
        mConnection.setIntentBuilder(mIntentBuilder);
        mConnection.bind(mServiceClass);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner)
    {
        mConnection.enqueueUnbind();
    }


    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        mConnection.unbind();
    }


    /**
     * Returns whether the connection is bound
     * @return whether the connection is bound
     */
    public boolean isBound()
    {
        return mConnection.isBound();
    }


    /**
     * Apply a function only if the connection is bound
     * @param fun function to execute
     */
    public <R> R applyBound(Function<T, R> fun, R defaultValue)
    {
        return mConnection.applyBoundFunction(fun, defaultValue);
    }


    /**
     * Apply a command only if the connection is bound
     * @param command command to execute
     */
    public void applyBound(Consumer<T> command)
    {
        mConnection.applyBound(command);
    }


    /**
     * Enqueues a command that will be executed when the connection is bound
     * @param command command to enqueue
     */
    public void enqueue(Consumer<T> command)
    {
        mConnection.enqueue(command);
    }
}
