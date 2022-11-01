package umu.software.activityrecognition.services.recordings;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


import com.google.common.collect.Lists;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import umu.software.activityrecognition.R;
import umu.software.activityrecognition.shared.lifecycles.ForegroundServiceLifecycle;
import umu.software.activityrecognition.shared.persistance.Authentication;
import umu.software.activityrecognition.shared.services.LifecycleService;


/**
 * Service to transfer directory in between URIs
 * Supported actions:
 * - ACTION_TRANSFER transfer directories
 *      Uses extras: EXTRA_URI_FROM, EXTRA_URI_TO. URI objects
 *      Optional: EXTRA_AUTH_FROM, EXTRA_AUTH_TO. Authentication objects.
 *
 * - ACTION_SHUTDOWN interrupts transfer and shut downs the service
 */
public class TransferService extends LifecycleService implements Observer<List<WorkInfo>>
{

    public static final String ACTION_TRANSFER  = "umu.software.activityrecognition.ACTION_TRANSFER";
    public static final String EXTRA_URI_FROM   = "EXTRA_URI_FROM";
    public static final String EXTRA_URI_TO     = "EXTRA_URI_TO";
    public static final String EXTRA_AUTH_FROM   = "EXTRA_AUTH_FROM";
    public static final String EXTRA_AUTH_TO     = "EXTRA_AUTH_TO";

    public static final String ACTION_SHUTDOWN  = "umu.software.activityrecognition.ACTION_SHUTDOWN";


    public static final String WORK_ID = "umu.software.activityrecognition.TransferService";


    private ForegroundServiceLifecycle mForegroundObserver;
    private int mPendingTransfers = 0;

    private final int mNotificationPathLength = 3;


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        mForegroundObserver = new ForegroundServiceLifecycle(
                this,
                builder ->
                {
                    builder.setContentTitle(getString(R.string.notification_transfer_title))
                            .setSmallIcon(R.mipmap.ic_watch_round)
                            .setGroup(getString(R.string.notification_group_id));
                }

        );


        getLifecycle().addObserver(mForegroundObserver);




        registerAction(this::onPerformTransfer,
                ACTION_TRANSFER,
                EXTRA_URI_FROM, EXTRA_URI_TO
        );
        registerAction(this::onShutdown, ACTION_SHUTDOWN);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        WorkManager
                .getInstance(this)
                .cancelAllWorkByTag(WORK_ID);
    }

    private void onShutdown(@Nullable Intent intent)
    {
        stopSelf();
    }



    private void onPerformTransfer(Intent intent)
    {
        URI from = (URI) intent.getSerializableExtra(EXTRA_URI_FROM);
        URI to = (URI) intent.getSerializableExtra(EXTRA_URI_TO);
        Authentication authFrom = (Authentication) intent.getSerializableExtra(EXTRA_AUTH_FROM);
        Authentication authTo = (Authentication) intent.getSerializableExtra(EXTRA_AUTH_TO);


        Data transferData = TransferWorker.createInputData(from, to, authFrom, authTo);
        Constraints transferConstraints = TransferWorker.createConstraints(from, to, authFrom, authTo);




        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TransferWorker.class)
                .setConstraints(transferConstraints)
                .setInputData(transferData)
                .build();




        WorkContinuation work = WorkManager
                .getInstance(this)
                .beginUniqueWork(
                        WORK_ID,
                        ExistingWorkPolicy.APPEND,
                        workRequest
                );
        work.getWorkInfosLiveData().observe(this, this);
        work.enqueue();

        mPendingTransfers += 1;
    }



    @Override
    public void onChanged(List<WorkInfo> workList)
    {
        if (workList.size() <= 0) // This happens when WorkContnuation s initialized
            return;


        WorkInfo workInfo = workList.get(0);

        switch (workInfo.getState())
        {

            case FAILED:
            case SUCCEEDED:
                mPendingTransfers--;
                if (mPendingTransfers <= 0)
                    stopSelf();
                return;

            case ENQUEUED:
                    break;

            case RUNNING:


                Data progressData = workInfo.getProgress();


                if (!progressData.getBoolean(TransferWorker.IS_PROGRESS_DATA, false))
                    return;



                int progress = progressData.getInt(TransferWorker.PROGRESS, 0);
                int maxProgress = progressData.getInt(TransferWorker.MAX_PROGRESS, 0);
                String currentFile = progressData.getString(TransferWorker.TRANSFERRED_FILENAME);

                if (currentFile == null) return;

                mForegroundObserver.updateNotification(builder -> {

                    List<String> elems = Lists.newLinkedList(Arrays.asList(currentFile.split(File.separator)));
                    while (elems.size() > mNotificationPathLength)
                        elems.remove(0);
                    if (elems.size() == mNotificationPathLength) elems.add(0, "...");

                    String filePath = String.join(File.separator, elems);


                    String contentText;
                    if (mPendingTransfers > 1)
                        contentText = String.format("Jobs: (%s) Progress (%s/%s)%s%s%s", mPendingTransfers, progress, maxProgress, System.lineSeparator(), filePath, System.lineSeparator());
                    else
                        contentText = String.format("Progress (%s/%s)%s%s", progress, maxProgress, System.lineSeparator(), filePath);


                    PendingIntent stopPendingIntent = PendingIntent.getService(
                            this,
                            10047,
                            new Intent(this, TransferService.class).setAction(ACTION_SHUTDOWN),
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    NotificationCompat.Action closeAction = new NotificationCompat.Action.Builder(null, getString(R.string.stop), stopPendingIntent).build();

                    builder.setGroup(getString(R.string.notification_group_id))
                            .setSmallIcon(R.mipmap.ic_watch_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_watch_round))
                            .setContentTitle(getString(R.string.notification_transfer_title))
                            .addAction(closeAction)
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setContentText(contentText);
                });

        }


    }
}
