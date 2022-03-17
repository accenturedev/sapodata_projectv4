package com.sap.appodatav4.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;
import com.sap.appodatav4.mdui.EntitySetListActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

public class OfflineSyncWorker extends OfflineBaseWorker {

    public OfflineSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        localContext = context;
    }

    private final Context localContext;
    private static final Logger logger = LoggerFactory.getLogger(OfflineSyncWorker.class);
    private static int startPointForSync = 0;
    public static int getStartPointForSync() {
        return startPointForSync;
    }

    private final OfflineProgressListener progressListener = new OfflineProgressListener() {
        @Override
        public void updateProgress(int currentStep, int totalSteps) {
            int requestID = (int) System.currentTimeMillis();
            Intent intent = new Intent(localContext, EntitySetListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(localContext, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationManager.notify(
                    OFFLINE_NOTIFICATION_CHANNEL_INT_ID,
                    createNotification(totalSteps, currentStep, pendingIntent)
            );
        }

        @Override
        public int getStartPoint() {
            return startPointForSync;
        }

        @Override
        public WorkerType getWorkerType() {
            return WorkerType.SYNC;
        }
    };

    private String errorMessage = null;
    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(new ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification(100, 0, null)));
        OfflineWorkerUtil.addProgressListener(progressListener);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        if (OfflineWorkerUtil.getOfflineODataProvider() != null) {
            logger.info("Uploading data...");
            startPointForSync = 0;
            OfflineODataProvider provider = OfflineWorkerUtil.getOfflineODataProvider();
            provider.upload(
                () -> {
                    logger.info("Downloading data...");
                    startPointForSync = progressListener.totalStepsForTwoProgresses / 2;
                    provider.download(
                        () -> {
                            countDownLatch.countDown();
                        },
                        exception -> {
                            countDownLatch.countDown();
                            errorMessage = (exception.getMessage() == null)? "Unknown offline sync error when downloading data." : exception.getMessage();
                            logger.error(errorMessage);
                        }
                    );
                },
                exception -> {
                    countDownLatch.countDown();
                    errorMessage = (exception.getMessage() == null)? "Unknown offline sync error when uploading data." : exception.getMessage();
                    logger.error(errorMessage);
                }
            );
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        OfflineWorkerUtil.removeProgressListener(progressListener);
        if (errorMessage != null) {
            logger.error("Offline sync error: " + errorMessage);
            return Result.failure(new Data.Builder().putString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL, errorMessage).build());
        } else {
            return Result.success();
        }
    }
}