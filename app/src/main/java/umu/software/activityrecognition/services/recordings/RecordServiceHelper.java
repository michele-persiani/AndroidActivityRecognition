package umu.software.activityrecognition.services.recordings;



import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.function.Consumer;

import umu.software.activityrecognition.data.dataframe.RowParcelable;
import umu.software.activityrecognition.shared.services.ServiceConnectionHandler;


/**
 * Helper class to start/stop/save using RecordService
 */
public class RecordServiceHelper
{
    private final Context mContext;


    RecordServiceHelper(Context context)
    {
        mContext = context;
    }


    public static RecordServiceHelper newInstance(Context context)
    {
        return new RecordServiceHelper(context.getApplicationContext());
    }


    /**
     * Start this service as a Service or ForegroundService depending on the system's version
     * @param intentBuilder builder for the starting intent. Used to set the extras
     */
    public void startRecording(@Nullable Consumer<Intent> intentBuilder)
    {
        Intent intent =  new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_START_RECORDING);
        if (intentBuilder != null)
            intentBuilder.accept(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(mContext, intent);
        else
            mContext.startService(intent);
    }


    /**
     * Stops RecordService
     */
    public void stopRecording()
    {
        stopRecurrentSave();
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_STOP_RECORDING);
        mContext.startService(intent);
    }

    /**
     * Bind RecordService service with the given ServiceConnection
     * @return whether the service got bound
     */
    public ServiceConnectionHandler<RecordService.Binder> bind()
    {
        return new ServiceConnectionHandler<RecordService.Binder>(mContext).bind(RecordService.class);
    }

    /**
     * Start the saveZipClearFiles() functionality of RecordService
     */
    public void saveZipClearFiles()
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_SAVE_ZIP_CLEAR);
        mContext.startService(intent);
    }


    /**
     * Sets the classification label for the recordings
     */
    public void setSensorsLabel(@Nullable String classification)
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_SET_CLASSIFICATION);
        intent.putExtra(RecordService.EXTRA_SENSOR_LABEL, classification);
        mContext.startService(intent);
    }



    /**
     * Start to recurrently save the sensor readings
     * @param intentBuilder builder for the intent recurrently broadcasted. Used to set the extras.
     *                      Can be null  to use values from the preferences
     */
    public void startRecurrentSave(@Nullable Consumer<Intent> intentBuilder)
    {
        stopRecurrentSave();
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_START_RECURRENT_SAVE);
        if (intentBuilder != null)
            intentBuilder.accept(intent);
        mContext.startService(intent);

    }

    /**
     * Start recurrent save using values from the preferences
     */
    public void startRecurrentSave()
    {
        startRecurrentSave(null);
    }

    /**
     * Stops recurrently sending intents to save the sensor readings
     */
    public void stopRecurrentSave()
    {
        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_STOP_RECURRENT_SAVE);
        mContext.startService(intent);
    }



    /**
     * Adds an accumulator to those being managed
     * @param accumulatorId the id of the added broadcast receiver
     * @param callback callback notified on whether the receiver was successfully registered
     */
    public void setupBroadcastReceiver(String accumulatorId, @Nullable Consumer<Boolean> callback)
    {
        ServiceConnectionHandler<RecordService.Binder> conn = new ServiceConnectionHandler<>(mContext);
        conn.bind(RecordService.class)
                .enqueue(binder -> {
                    boolean success = binder.getService().setupBroadcastReceiver(accumulatorId);
                    if (callback != null)
                        callback.accept(success);
                })
                .enqueueUnbind();
    }


    /**
     * Removes an accumulator from those being managed
     * @param accumulatorId the id of the broadcast receiver to remove
     */
    public void removeBroadcastReceiver(String accumulatorId, @Nullable Consumer<Boolean> callback)
    {
        ServiceConnectionHandler<RecordService.Binder> conn = new ServiceConnectionHandler<>(mContext);
        conn.bind(RecordService.class)
                .enqueue(binder -> {
                    boolean success = binder.getService().removeBroadcastReceiver(accumulatorId);
                    if (callback != null)
                        callback.accept(success);
                })
                .enqueueUnbind();
    }

    /**
     * Sends an event to the accumulator with the given accumulatorId. The accumulator must have been
     * added before through setupBroadcastReceiver()
     * @param accumulatorId the id of the accumulator to sent the event to
     * @param eventBuilder builder to construct the event to send
     */
    public void recordEvent(String accumulatorId, Consumer<RowParcelable> eventBuilder)
    {
        RowParcelable event = new RowParcelable();
        eventBuilder.accept(event);

        Intent intent = new Intent(mContext, RecordService.class);
        intent.setAction(RecordService.ACTION_RECORD_EVENT);
        intent.putExtra(RecordService.EXTRA_ACCUMULATOR_ID, accumulatorId);
        intent.putExtra(RecordService.EXTRA_EVENT, event);

        mContext.sendBroadcast(intent);
    }

}
