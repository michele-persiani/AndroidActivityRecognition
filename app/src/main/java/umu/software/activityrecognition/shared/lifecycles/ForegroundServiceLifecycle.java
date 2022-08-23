package umu.software.activityrecognition.shared.lifecycles;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import umu.software.activityrecognition.shared.util.AndroidUtils;

/**
 * Lifecycle observer that starts a service as foreground
 */
public class ForegroundServiceLifecycle implements DefaultLifecycleObserver
{
    private final Consumer<NotificationCompat.Builder> mNotificationBuilder;
    private final String mChannelName;
    private final int mNotificationId;
    private final Service mService;



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
                new Random().nextInt(100000) + 100000,
                String.format("NotificationChannel-%s", UUID.randomUUID()),
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


    public void setupForegroundService()
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
        mService.startForeground(mNotificationId, n);

    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner)
    {
        setupForegroundService();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)
    {
        mService.stopForeground(true);
    }
}
