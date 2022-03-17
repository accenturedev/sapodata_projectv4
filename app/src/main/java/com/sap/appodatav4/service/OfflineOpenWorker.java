package com.sap.appodatav4.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.*;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;
import com.sap.appodatav4.app.MainBusinessActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

/**
 * Represents the worker to open the offline database.
 */
public class OfflineOpenWorker extends OfflineBaseWorker {

    public OfflineOpenWorker(
        @NonNull Context context,
        @NonNull WorkerParameters params) {
        super(context, params);
        localContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(localContext);
    }

    private static final Logger logger = LoggerFactory.getLogger(OfflineOpenWorker.class);
    private final Context localContext;
    private int result = 0;
    private String errorMessage = null;
    private final SharedPreferences sharedPreferences;
    private static int startPointForOpen = 0;

    public static int getStartPointForOpen() {
        return startPointForOpen;
    }

    private final OfflineProgressListener progressListener = new OfflineProgressListener() {
        @Override
        public void updateProgress(int currentStep, int totalSteps) {
            int requestID = (int) System.currentTimeMillis();
            Intent intent = new Intent(localContext, MainBusinessActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(localContext, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            notificationManager.notify(
                    OFFLINE_NOTIFICATION_CHANNEL_INT_ID,
                    createNotification(totalSteps, currentStep, pendingIntent)
            );
        }

        @Override
        public int getStartPoint() {
            return startPointForOpen;
        }

        @Override
        public WorkerType getWorkerType() {
            return WorkerType.OPEN;
        }
    };

    @NonNull
    @Override
    public Result doWork() {
        setForegroundAsync(new ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification(100, 0, null)));
        OfflineWorkerUtil.addProgressListener(progressListener);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        if (OfflineWorkerUtil.getOfflineODataProvider() != null) {
            startPointForOpen = 0;
            OfflineODataProvider provider = OfflineWorkerUtil.getOfflineODataProvider();
            provider.open(
                    () -> {
                        logger.info("Offline provider open succeeded.");
                        if (OfflineWorkerUtil.userSwitchFlag) {
                            startPointForOpen = progressListener.totalStepsForTwoProgresses / 2;
                            provider.download(
                                    () -> {
                                        sharedPreferences.edit()
                                                .putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, true)
                                                .apply();
                                        logger.info("Offline provider download succeeded.");
                                        countDownLatch.countDown();
                                    },
                                    exception -> {
                                        errorMessage = (exception.getMessage() == null)? "Unknown offline sync error when downloading data." : exception.getMessage();
                                        logger.error(errorMessage);
                                        ConnectivityManager connectivityManager =
                                                (ConnectivityManager) (localContext.getSystemService(
                                                        Context.CONNECTIVITY_SERVICE));
                                        Network activeNetwork = connectivityManager.getActiveNetwork();
                                        NetworkCapabilities capabilities =
                                                connectivityManager.getNetworkCapabilities(activeNetwork);
                                        if (capabilities == null) {
                                            result = -1;
                                        } else {
                                            result = exception.getErrorCode();
                                        }
                                        countDownLatch.countDown();
                                    }
                            );
                        } else {
                            sharedPreferences.edit()
                                    .putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, true)
                                    .apply();
                            countDownLatch.countDown();
                        }
                    },
                    exception -> {
                        errorMessage = (exception.getMessage() == null)? "Unknown offline sync error when init opening data." : exception.getMessage();
                        logger.error(errorMessage);
                        ConnectivityManager connectivityManager =
                                (ConnectivityManager) (localContext.getSystemService(
                                        Context.CONNECTIVITY_SERVICE));
                        Network activeNetwork = connectivityManager.getActiveNetwork();
                        NetworkCapabilities capabilities =
                                connectivityManager.getNetworkCapabilities(activeNetwork);
                        if (capabilities == null) {
                            result = -1;
                        } else {
                            result = exception.getErrorCode();
                        }
                        countDownLatch.countDown();
                    }
            );
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        OfflineWorkerUtil.removeProgressListener(progressListener);
        return (result == 0)? Result.success() : Result.failure(new Data.Builder()
                                                                        .putInt(OfflineWorkerUtil.OUTPUT_ERROR_KEY, result)
                                                                        .putString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL, errorMessage)
                                                                        .build());
    }
}
