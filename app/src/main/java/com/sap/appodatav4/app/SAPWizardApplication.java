package com.sap.appodatav4.app;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.sap.appodatav4.service.OfflineWorkerUtil;
import com.sap.cloud.mobile.foundation.settings.SharedDeviceService;
import com.sap.appodatav4.repository.RepositoryFactory;
import com.sap.cloud.mobile.foundation.mobileservices.MobileService;
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer;
import java.util.ArrayList;
import java.util.List;
import com.sap.cloud.mobile.foundation.logging.LoggingService;
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy;

public class SAPWizardApplication extends Application {

    public boolean isApplicationUnlocked = false;
    public static SharedPreferences sp;

    public static final String KEY_LOG_SETTING_PREFERENCE = "key.log.settings.preference";
    private static final String OFFLINE_APP_ENCRYPTION_CONSTANT = "34dab53fc060450280faeed44a36571b";

    /**
     * Application-wide RepositoryFactory
     */
    private RepositoryFactory repositoryFactory;
    /**
     * Returns the application-wide repository factory
     *
     * @return the repository factory
     */
    public RepositoryFactory getRepositoryFactory() {
        return repositoryFactory;
    }

    /**
     * Clears all user-specific data from the application, essentially resetting
     * it to its initial state.
     *
     * If client code wants to handle the reset logic of a service, here is an example:
     * 
     *     SDKInitializer.INSTANCE.resetServices( service -> {
     *             if(service instanceof PushService) {
     *                 PushService.unregisterPushSync(new RemoteNotificationClient.CallbackListener() {
     *                     @Override
     *                     public void onSuccess() {
     *
     *                     }
     *
     *                     @Override
     *                     public void onError(@NonNull Throwable throwable) {
     *
     *                     }
     *                 });
     *                 return true;
     *             } else {
     *                 return false;
     *             }
     *         });
     */
    public void resetApp() {
        sp.edit().clear().apply();
        isApplicationUnlocked = false;
        repositoryFactory.reset();
        SDKInitializer.INSTANCE.resetServices(null);
        OfflineWorkerUtil.resetOffline(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        repositoryFactory = new RepositoryFactory();
        initService();
    }

    private void initService() {
        List<MobileService> services = new ArrayList<>();
        LoggingService loggingService = new LoggingService(false);
        loggingService.setPolicy(new LogPolicy(true, 0, "WARN", 0, 4));
        loggingService.setLogToConsole(true);
        services.add(loggingService);

        services.add(new SharedDeviceService(OFFLINE_APP_ENCRYPTION_CONSTANT));

        SDKInitializer.INSTANCE.start(this, services.toArray(new MobileService[0]), null);
    }


}
