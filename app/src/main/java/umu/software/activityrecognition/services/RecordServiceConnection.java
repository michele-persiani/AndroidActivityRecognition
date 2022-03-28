package umu.software.activityrecognition.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.function.Function;

public class RecordServiceConnection implements ServiceConnection
{

    private RecordService mService = null;
    Function<RecordService, ?> mCallback = null;


    public RecordServiceConnection(Function<RecordService, ?> callback)
    {
        setCallback(callback);
    }


    public RecordServiceConnection()
    {
    }



    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder)
    {
        mService = ((RecordService.RecordBinder) iBinder).getService();

        if (mCallback != null)
            mCallback.apply(mService);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        mService = null;
    }


    public RecordServiceConnection setCallback(Function<RecordService, ?> callback)
    {
        mCallback = callback;
        return this;
    }


    public boolean isConnected()
    {
        return mService != null;
    }


    public <T> T apply(Function<RecordService, T> operation)
    {
        if (!isConnected())
            return null;
        return operation.apply(mService);
    }

    public boolean bind(Context context)
    {
        Intent intent = new Intent(context, RecordService.class);
        return context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
}
