package umu.software.activityrecognition.common.lifecycles;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.function.Consumer;

import umu.software.activityrecognition.common.AndroidUtils;

public class ForegroundServiceLifecycle implements LifecycleElement
{
    private final Consumer<NotificationCompat.Builder> mNotificationBuilder;
    private final String mChannelName;
    private final int mId;

    public ForegroundServiceLifecycle(int id, String notificationChannel, Consumer<NotificationCompat.Builder> notificationBuilder)
    {
        mId = id;
        mChannelName = notificationChannel;
        this.mNotificationBuilder = notificationBuilder;
    }

    private void initializeForegroundService(Service service)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                mChannelName,
                mChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
        );

        AndroidUtils
                .getNotificationManager(service)
                .createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, mChannelName);
        mNotificationBuilder.accept(builder);
        Notification n = builder.build();

        service.startForeground(mId, n);
    }

    @Override
    public void onCreate(Context context)
    {
        Service service = (Service) context;
        initializeForegroundService(service);
    }

    @Override
    public void onStart(Context context)
    {
        Service service = (Service) context;
        initializeForegroundService(service);
    }

    @Override
    public void onStop(Context context)
    {

    }

    @Override
    public void onDestroy(Context context)
    {

    }
}
