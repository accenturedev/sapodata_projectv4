package com.sap.appodatav4.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.sap.appodatav4.R;

public abstract class OfflineBaseWorker extends Worker {
    protected NotificationManager notificationManager;
    public static final String OFFLINE_NOTIFICATION_CHANNEL_ID = "offline_wizard_channel";
    public static final String OFFLINE_NOTIFICATION_CHANNEL_NAME = "SAP Wizard Channel";
    public static final int OFFLINE_NOTIFICATION_CHANNEL_INT_ID = 1;

    public OfflineBaseWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * To send notification, Oreo and later versions (API 26+) require a notification channel
     */
    private void createNotificationChannel() {
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(OFFLINE_NOTIFICATION_CHANNEL_ID, OFFLINE_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    protected Notification createNotification(
            int maxStep,
            int currentStep,
            @Nullable PendingIntent pendingIntent
    ) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), OFFLINE_NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Syncing offline data...")
                        .setSmallIcon(R.drawable.ic_sync)
                        .setWhen(System.currentTimeMillis())
                        .setOngoing(true)
                        .setProgress(maxStep, currentStep, false);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        return builder.build();
    }
}
