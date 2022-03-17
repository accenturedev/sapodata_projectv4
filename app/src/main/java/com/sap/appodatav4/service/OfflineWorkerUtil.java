package com.sap.appodatav4.service;

import com.sap.cloud.mobile.odata.offline.OfflineODataDefiningQuery;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;
import com.sap.cloud.mobile.odata.offline.OfflineODataProviderOperationProgress;
import android.content.Context;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.sap.cloud.android.odata.container.Container;
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry;
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate;
import com.sap.cloud.mobile.foundation.common.ClientProvider;
import com.sap.cloud.mobile.foundation.common.SettingsProvider;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import com.sap.cloud.mobile.odata.core.AndroidSystem;
import com.sap.cloud.mobile.odata.core.Logger;
import com.sap.cloud.mobile.odata.core.LoggerFactory;
import com.sap.cloud.mobile.odata.offline.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class OfflineWorkerUtil {
    private OfflineWorkerUtil() {}

    /*
     * Offline OData Provider
     */
    private static OfflineODataProvider offlineODataProvider = null;

    public static OfflineODataProvider getOfflineODataProvider() {
        return offlineODataProvider;
    }

    public static void resetOfflineODataProvider() {
        offlineODataProvider = null;
    }

    private static OneTimeWorkRequest openRequest = null;

    public static OneTimeWorkRequest getOpenRequest() {
        return openRequest;
    }

    public static void resetOpenRequest() {
        openRequest = null;
    }

    private static OneTimeWorkRequest syncRequest = null;

    public static OneTimeWorkRequest getSyncRequest() {
        return syncRequest;
    }

    public static void resetSyncRequest() {
        syncRequest = null;
    }
    private static final Logger logger = LoggerFactory.getLogger(OfflineWorkerUtil.class.toString());
    private static final HashSet<OfflineProgressListener> progressListeners = new HashSet<>();
    public static final String OFFLINE_OPEN_WORKER_UNIQUE_NAME = "offline_init_sync_worker";

    public static final String OFFLINE_SYNC_WORKER_UNIQUE_NAME = "offline_sync_worker";

    public static final String OUTPUT_ERROR_KEY = "output.error";

    public static final String OUTPUT_ERROR_DETAIL = "output.error.detail";

    /** Name of the offline data file on the application file space */
    private static final String OFFLINE_DATASTORE = "OfflineDataStore";

    public static final String OFFLINE_DATASTORE_ENCRYPTION_KEY = "Offline_DataStore_EncryptionKey";

    /** Header name for application version */
    private static final String APP_VERSION_HEADER = "X-APP-VERSION";

    /** The preference to say whether offline is initialized. */
    public static final String PREF_OFFLINE_INITIALIZED = "pref.offline.db.initialized";

    /** The preference to say whether app is in foreground service. */
    public static final String PREF_FOREGROUND_SERVICE = "pref.foreground.service";

    /** The preference to say whether app just deleted registration. */
    public static final String PREF_DELETE_REGISTRATION = "pref.delete.registration";

    public static boolean userSwitchFlag = false;

    /*
     * OData service for interacting with local OData Provider
     */
    private static Container container;

    @NonNull
    public static Container getContainer() {
        if (container == null) {
            throw new NullPointerException("container was not initialized.");
        }
        return container;
    }
    /*
     * Connection ID of Mobile Application
     */
    public static final String CONNECTION_ID_CONTAINER = "sapodatav4";

    public static void addProgressListener(@NonNull OfflineProgressListener listener) {
        progressListeners.add(listener);
    }

    public static void removeProgressListener(@NonNull OfflineProgressListener listener) {
        progressListeners.remove(listener);
    }

    private static final OfflineODataProviderDelegate delegate = new OfflineODataProviderDelegate() {
        @Override
        public void updateOpenProgress(@NonNull OfflineODataProvider offlineODataProvider,
                @NonNull OfflineODataProviderOperationProgress offlineODataProviderOperationProgress) {
            notifyListeners(offlineODataProvider, offlineODataProviderOperationProgress);
        }

        @Override
        public void updateDownloadProgress(@NonNull OfflineODataProvider offlineODataProvider,
                @NonNull OfflineODataProviderDownloadProgress offlineODataProviderDownloadProgress) {
            notifyListeners(offlineODataProvider, offlineODataProviderDownloadProgress);
        }

        @Override
        public void updateUploadProgress(@NonNull OfflineODataProvider offlineODataProvider,
                @NonNull OfflineODataProviderOperationProgress offlineODataProviderOperationProgress) {
            notifyListeners(offlineODataProvider, offlineODataProviderOperationProgress);
        }

        @Override
        public void updateFailedRequest(@NonNull OfflineODataProvider offlineODataProvider,
                @NonNull OfflineODataFailedRequest offlineODataFailedRequest) {

        }

        @Override
        public void updateSendStoreProgress(@NonNull OfflineODataProvider offlineODataProvider,
                @NonNull OfflineODataProviderOperationProgress offlineODataProviderOperationProgress) {
            notifyListeners(offlineODataProvider, offlineODataProviderOperationProgress);
        }

        private void notifyListeners(
                OfflineODataProvider offlineODataProvider,
            OfflineODataProviderOperationProgress offlineODataProviderOperationProgress
        ) {
            logger.debug("Progress " + offlineODataProviderOperationProgress.getCurrentStepNumber()
                    + " out of " + offlineODataProviderOperationProgress.getTotalNumberOfSteps());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                for (OfflineProgressListener progressListener : progressListeners) {
                    progressListener.onOfflineProgress(offlineODataProvider,
                            offlineODataProviderOperationProgress);
                }
            });
            executor.shutdown();
        }
    };

    /*
     * Create OfflineODataProvider
     * This is a blocking call, no data will be transferred until open, download, upload
     */
    public static void initializeOffline(
        Context context,
        AppConfig appConfig,
        boolean runtimeMultipleUserMode
    ) {
        if (offlineODataProvider != null) return;
        if (FlowContextRegistry.getFlowContext().getCurrentUserId() == null ||
                FlowContextRegistry.getFlowContext().getCurrentUserId().isEmpty())
            throw new IllegalStateException("Current user not ready yet.");

        AndroidSystem.setContext(context);
        String serviceUrl = appConfig.getServiceUrl();
        try {
            URL url = new URL(serviceUrl + CONNECTION_ID_CONTAINER);
            OfflineODataParameters offlineODataParameters = new OfflineODataParameters();
            offlineODataParameters.setEnableRepeatableRequests(true);
            offlineODataParameters.setStoreName(OFFLINE_DATASTORE);
            offlineODataParameters.setCurrentUser(FlowContextRegistry.getFlowContext().getCurrentUserId());
            offlineODataParameters.setForceUploadOnUserSwitch(runtimeMultipleUserMode);
            String encryptionKey;
            if (runtimeMultipleUserMode) {
                encryptionKey = UserSecureStoreDelegate.getInstance().getOfflineEncryptionKey();
            } else { //If is single user mode, create and save a key into user secure store for accessing offline DB
                if (UserSecureStoreDelegate.getInstance().getData(OFFLINE_DATASTORE_ENCRYPTION_KEY) == null) {
                    byte[] bytes = new byte[32];
                    SecureRandom random = new SecureRandom();
                    random.nextBytes(bytes);
                    encryptionKey = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    UserSecureStoreDelegate.getInstance().saveData(OFFLINE_DATASTORE_ENCRYPTION_KEY, encryptionKey);
                    Arrays.fill(bytes, (byte) 0);
                } else {
                    encryptionKey = UserSecureStoreDelegate.getInstance().getData(OFFLINE_DATASTORE_ENCRYPTION_KEY);
                }
            }
            offlineODataParameters.setStoreEncryptionKey(encryptionKey);

            // Set the default application version
            Map customHeaders = offlineODataParameters.getCustomHeaders();
            customHeaders.put(APP_VERSION_HEADER, SettingsProvider.get().getApplicationVersion());
            // In case of offlineODataParameters.customHeaders returning a new object if customHeaders from offlineODataParameters is null, set again as below
            offlineODataParameters.setCustomHeaders(customHeaders);

            offlineODataProvider = new OfflineODataProvider(url, offlineODataParameters, ClientProvider.get(), delegate);
            OfflineODataDefiningQuery orderItemSetQuery = new OfflineODataDefiningQuery("OrderItemSet", "OrderItemSet", false);
            offlineODataProvider.addDefiningQuery(orderItemSetQuery);
            OfflineODataDefiningQuery orderSetQuery = new OfflineODataDefiningQuery("OrderSet", "OrderSet", false);
            offlineODataProvider.addDefiningQuery(orderSetQuery);
            container = new Container(offlineODataProvider);
        } catch (Exception e) {
            logger.error("Exception encountered setting up offline store: " + e.getMessage());
        }
    }

    /*
     * Close and remove offline data store
     */
    public static void resetOffline(Context context) {
        try {
            AndroidSystem.setContext(context);
            if (offlineODataProvider != null) {
                offlineODataProvider.close();
            }
            OfflineODataProvider.clear(OFFLINE_DATASTORE);
        } catch (OfflineODataException e) {
            logger.error("Unable to reset Offline Data Store. Encountered exception: " + e.getMessage());
        } finally {
            offlineODataProvider = null;
        }
        progressListeners.clear();
    }

    public static void open(Context context) {
        if (FlowContextRegistry.getFlowContext().getCurrentUserId() == null) {
            throw new IllegalStateException("Current user not ready yet.");
        }

        if (!userSwitchFlag && openRequest != null) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build();

        openRequest = new OneTimeWorkRequest.Builder(OfflineOpenWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            OFFLINE_OPEN_WORKER_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            openRequest
        );
    }

    /**
     * Synchronize local offline data store with Server
     * Upload - local changes
     * Download - server changes
     */
    public static void sync(Context context) {
        if (syncRequest != null) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build();

        syncRequest = new OneTimeWorkRequest.Builder(OfflineSyncWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            OFFLINE_SYNC_WORKER_UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        );
    }
}
