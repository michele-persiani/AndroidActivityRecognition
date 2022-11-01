package umu.software.activityrecognition.shared.lifecycles;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.function.Consumer;

import umu.software.activityrecognition.shared.util.AndroidUtils;

/**
 * Lifecycle observer that starts a service as foreground
 */
public class ForegroundServiceLifecycle implements DefaultLifecycleObserver
{
    public final static int DEFAULT_NOTIFICATION_CHANNEL_ID = 100543;
    public final static String DEFAULT_NOTIFICATION_CHANNEL_NAME ="ForegroundServiceNotificationChannel";

    private final Consumer<NotificationCompat.Builder> mNotificationBuilder;
    private final String mChannelName;
    private final int mNotificationId;
    private final Service mService;
    private NotificationCompat.Builder mBuilder;


    public ForegroundServiceLifecycle(Service service, int id, String notificationChannel, Consumer<NotificationCompat.Builder> notificationBuilder)
    {
        mNotificationId = id;
        mChannelName = notificationChannel;
        mNotificationBuilder = notificationBuilder;
        mService = service;
    }


    public ForegroundServiceLifecycle(Service service, Consumer<NotificationCompat.Builder> notificationBuilder)
    {
        this(
                service,
                DEFAULT_NOTIFICATION_CHANNEL_ID,
                DEFAULT_NOTIFICATION_CHANNEL_NAME,
                notificationBuilder
        );
    }

    public int getNotificationId()
    {
        return mNotificationId;
    }


    public String getNotificationChannel()
    {
        return mChannelName;
    }



    public void updateNotification(Consumer<NotificationCompat.Builder> notificationBuilder)
    {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, mChannelName);
        notificationBuilder.accept(builder);
        builder.setVibrate(null);
        Notification n = builder.build();
        AndroidUtils
                .getNotificationManager(mService)
                .notify(mNotificationId, n);
    }


    public void updateNotification(String contentText)
    {

        NotificationCompat.Builder builder = mBuilder;
        mBuilder.setContentText(contentText);
        Notification n = builder.build();
        AndroidUtils
                .getNotificationManager(mService)
                .notify(mNotificationId, n);
    }

    public void cancelNotification()
    {
        AndroidUtils.getNotificationManager(mService).cancel(mNotificationId);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner)
    {
        NotificationChannel channel = new NotificationChannel(
                mChannelName,
                mChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
        );

        AndroidUtils
                .getNotificationManager(mService)
                .createNotificationChannel(channel);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, mChannelName);
        mNotificationBuilder.accept(builder);
        Notification n = builder.build();
        mBuilder = builder;
        mService.startForeground(mNotificationId, n);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        cancelNotification();
        mService.stopForeground(true);
    }
}
