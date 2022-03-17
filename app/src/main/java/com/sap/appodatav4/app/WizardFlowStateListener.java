package com.sap.appodatav4.app;

import android.content.Intent;
import java.util.Objects;
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry;
import com.sap.cloud.mobile.flowv2.ext.FlowStateListener;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;
import com.sap.cloud.mobile.foundation.settings.policies.ClientPolicies;
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy;
import ch.qos.logback.classic.Level;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import com.sap.appodatav4.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.cloud.mobile.odata.offline.OfflineODataException;
import com.sap.appodatav4.service.OfflineWorkerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WizardFlowStateListener extends FlowStateListener {
    private static Logger logger = LoggerFactory.getLogger(WizardFlowStateListener.class);
    private final SAPWizardApplication application;

    public WizardFlowStateListener(@NotNull SAPWizardApplication application) {
        super();
        this.application = application;
    }

    @Override
    public void onAppConfigRetrieved(@NotNull AppConfig appConfig) {
        logger.debug(String.format("onAppConfigRetrieved: %s", appConfig.toString()));
    }

    @Override
    public void onApplicationReset() {
        this.application.resetApp();
    }

    @Override
    public void onApplicationLocked() {
        super.onApplicationLocked();
        application.isApplicationUnlocked = false;
    }

    @Override
    public void onFlowFinished(@Nullable String flowName) {
        if(flowName != null) {
            application.isApplicationUnlocked = true;
        }

        boolean userSwitchFlag = false;
        if (FlowContextRegistry.getFlowContext().getPreviousUserId() != null) {
            userSwitchFlag = !Objects.equals(
                    FlowContextRegistry.getFlowContext().getCurrentUserId(),
                    FlowContextRegistry.getFlowContext().getPreviousUserId());
        }

        if (userSwitchFlag || application.sp.getBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false)) {
            if (application.sp.getBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false)) {
                application.sp.edit().putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false).apply();
            }
            Intent intent = new Intent(application, MainBusinessActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            application.startActivity(intent);
        }
    }

    @Override
    public void onClientPolicyRetrieved(@NotNull ClientPolicies policies) {
        SharedPreferences sp = application.sp;
        String logString = sp.getString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, "");
        LogPolicy currentSettings;
        if (logString.isEmpty()) {
            currentSettings = new LogPolicy();
        } else {
            currentSettings = LogPolicy.createFromJsonString(logString);
        }

        LogPolicy logSettings = policies.getLogPolicy();
        if (!currentSettings.getLogLevel().equals(logSettings.getLogLevel()) || logString.isEmpty()) {
            sp.edit().putString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE,
                    logSettings.toString()).apply();
            LogPolicy.setRootLogLevel(logSettings);
            AppLifecycleCallbackHandler.getInstance().getActivity().runOnUiThread(() -> {
                Map mapping = new HashMap<Level, String>();
                mapping.put(Level.ALL, application.getString(R.string.log_level_path));
                mapping.put(Level.DEBUG, application.getString(R.string.log_level_debug));
                mapping.put(Level.INFO, application.getString(R.string.log_level_info));
                mapping.put(Level.WARN, application.getString(R.string.log_level_warning));
                mapping.put(Level.ERROR, application.getString(R.string.log_level_error));
                mapping.put(Level.OFF, application.getString(R.string.log_level_none));
                Toast.makeText(
                        application,
                        String.format(
                                application.getString(R.string.log_level_changed),
                                mapping.get(LogPolicy.getLogLevel(logSettings))
                        ),
                        Toast.LENGTH_SHORT
                ).show();
                logger.info(String.format(
                                application.getString(R.string.log_level_changed),
                                mapping.get(LogPolicy.getLogLevel(logSettings))
                        ));
            });
        }
    }

    @Override
    public void onOfflineEncryptionKeyReady(@Nullable String key) {
        logger.info("offline key ready.");
        boolean userSwitchFlag = false;
        if (!application.sp.getBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, false)) {
            if (FlowContextRegistry.getFlowContext().getPreviousUserId() != null) {
                userSwitchFlag = !Objects.equals(
                        FlowContextRegistry.getFlowContext().getCurrentUserId(),
                        FlowContextRegistry.getFlowContext().getPreviousUserId());
            }
        } else {
            userSwitchFlag = true;
            application.sp.edit().putBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, false).apply();
            OfflineWorkerUtil.resetOffline(application);
        }

        OfflineWorkerUtil.userSwitchFlag = userSwitchFlag;
        if (userSwitchFlag) {
            application.sp.edit()
                    .putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false)
                    .apply();
            application.getRepositoryFactory().reset();
            if (OfflineWorkerUtil.getOfflineODataProvider() != null) {
                try {
                    OfflineWorkerUtil.getOfflineODataProvider().close();
                    OfflineWorkerUtil.resetOfflineODataProvider();
                } catch (OfflineODataException e) {
                    logger.error("Cannot close database successfully!");
                }
            }
        }
    }
}
