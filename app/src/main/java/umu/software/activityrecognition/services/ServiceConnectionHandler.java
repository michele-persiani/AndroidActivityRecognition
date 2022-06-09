package umu.software.activityrecognition.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.google.common.collect.Queues;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Helper class to handle a ServiceConnection
 * @param <B> the class of the IBinder returned when binding to a service
 */
public class ServiceConnectionHandler<B extends IBinder> implements ServiceConnection
{
    private final Context mContext;
    private B mBinder;

    private final Queue<Consumer<B>> mCallbacks         = Queues.newArrayDeque();
    private boolean mBinding                            = false;
    private Consumer<Intent> mIntentBuilder             = null;
    private boolean mAutoRebind                         = false;
    private Class<? extends Service> mLastBoundServiceClass = null;

    public ServiceConnectionHandler(Context context)
    {
        mContext = context.getApplicationContext();
    }


    /**
     * Sets the builder for the intents used to bind the services. This function is not reset when
     * after binding
     * @param builder a function to initialize intents or null to reset it
     * @return this instance
     */
    public ServiceConnectionHandler<B> setIntentBuilder(@Nullable Consumer<Intent> builder)
    {
        mIntentBuilder = builder;
        return this;
    }

    /**
     * Sets whether the connection should rebind itself when onServiceDisconnected() is called
     * @param autoRebind whether to auto rebind
     * @return this instance
     */
    public ServiceConnectionHandler<B> setAutoRebind(boolean autoRebind)
    {
        mAutoRebind = autoRebind;
        return this;
    }

    /**
     * Binds the connection to a service. Has no effect if the connection is already bound to some service
     * @param serviceClass the class of the service to bind to
     * @return this instance
     */
    public synchronized ServiceConnectionHandler<B> bind(Class<? extends Service> serviceClass)
    {
        if (isBound() || isBinding())
            return this;
        mBinding = true;
        Intent intent = new Intent(mContext, serviceClass);
        if (mIntentBuilder != null)
            mIntentBuilder.accept(intent);
        mLastBoundServiceClass = serviceClass;
        mContext.bindService(intent, this, Service.BIND_AUTO_CREATE);
        return this;
    }

    /**
     * Unbinds from the service. If this connection is binding (isBinding() is true), append a function
     * unbind to the callbacks
     * @return this instance
     */
    public synchronized ServiceConnectionHandler<B> unbind()
    {
        if(!isBound())
            return this;
        if (isBinding()) {
            enqueue((binder) -> unbind());
            return this;
        }
        setAutoRebind(false);
        mContext.unbindService(this);
        return this;
    }

    /**
     *
     * @return the service's binder or null if the connection is not bound yet
     */
    public B getBinder()
    {
        return mBinder;
    }


    /**
     * Execute an operation on the service's binder immediately if the service is bound, or add it to
     * the list of callbacks, in this second case the function will be executed as soon as the service
     * will be bound
     * @param operation the function to perform
     * @return this instance
     */
    public synchronized ServiceConnectionHandler<B> enqueue(Consumer<B> operation)
    {
        if (isBound())
            operation.accept(getBinder());
        else
            mCallbacks.add(operation);
        return this;
    }


    /**
     * Apply a function only if the service is bound
     * @param fcn the function to apply to the service's binder
     * @param <R> the function's result, or null if the service was not bound
     * @return the function's result or null if the service is not bound
     */
    public synchronized <R> R applyBoundFunction(Function<B, R> fcn)
    {
        if (isBound())
            return fcn.apply(getBinder());
        else
            return null;
    }


    /**
     * Apply a function only if the service is bound. If the service is not bound nothing will be done
     * @param fcn the function to apply to the service's binder
     */
    public synchronized void applyBound(Consumer<B> fcn)
    {
        if (isBound())
            fcn.accept(getBinder());
    }

    /**
     *
     * @return whether the connection is bound
     */
    public synchronized boolean isBound()
    {
        return mBinder != null;
    }

    /**
     *
     * @return whether the connection is binding
     */
    public synchronized boolean isBinding()
    {
        return mBinding;
    }

    @Override
    public synchronized void onServiceConnected(ComponentName componentName, IBinder iBinder)
    {
        mBinding = false;
        mBinder = (B) iBinder;
        while (mCallbacks.size() > 0)
        {
            Consumer<B> op = mCallbacks.remove();
            op.accept(mBinder);
        }
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName componentName)
    {
        mBinder = null;
        if (mAutoRebind && mLastBoundServiceClass != null)
            bind(mLastBoundServiceClass);
        else
            mCallbacks.clear();
    }
}
