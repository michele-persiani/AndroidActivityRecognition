package umu.software.activityrecognition.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import umu.software.activityrecognition.services.RecordService;


/**
 * This component restart the RecordService if it stopped without an actual stop intent.
 * For example by the system
 */

public class RecordServiceStarter extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {

        int recurrentSaveSecs = intent.getIntExtra(
                RecordService.EXTRA_RECURRENT_SAVE_SECS,
                RecordService.DEFAULT_RECURRENT_SAVE_SECS // default value
        );
        RecordService.start(context, recurrentSaveSecs);
    }


    public static void broadcast(Context context)
    {
        broadcast(context, RecordService.DEFAULT_RECURRENT_SAVE_SECS);
    }


    public static void broadcast(Context context, int recurrentSaveSecs)
    {
        Intent intent = new Intent(context, RecordServiceStarter.class);
        intent.putExtra(RecordService.EXTRA_RECURRENT_SAVE_SECS, recurrentSaveSecs);
        context.sendBroadcast(intent);
    }
}
