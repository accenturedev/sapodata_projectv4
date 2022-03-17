package com.sap.appodatav4.mdui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sap.appodatav4.R;
import com.sap.appodatav4.app.SAPWizardApplication;
import com.sap.appodatav4.app.WelcomeActivity;

import com.sap.cloud.mobile.flowv2.core.Flow;
import com.sap.cloud.mobile.flowv2.core.FlowContext;
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry;
import com.sap.cloud.mobile.flowv2.core.FlowContextBuilder;
import com.sap.cloud.mobile.flowv2.model.FlowConstants;
import com.sap.cloud.mobile.flowv2.model.FlowType;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.preference.ListPreference;
import ch.qos.logback.classic.Level;
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sap.cloud.mobile.foundation.logging.LoggingService;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.sap.cloud.mobile.flowv2.core.DialogHelper;
import com.sap.cloud.mobile.foundation.mobileservices.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer;
import kotlin.jvm.JvmClassMappingKt;

/** This fragment represents the settings screen. */
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsFragment.class);
    private ListPreference logLevelPreference;
    private Preference logUploadPreference;
    private Preference changePasscodePreference;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        logLevelPreference = (ListPreference) findPreference(getString(R.string.log_level));
        prepareLogLevelSetting(logLevelPreference);
        // Upload log
        logUploadPreference = findPreference(getString(R.string.upload_log));
        logUploadPreference.setOnPreferenceClickListener(preference -> {
            logUploadPreference.setEnabled(false);
            LoggingService loggingService = SDKInitializer.INSTANCE
                    .getService(JvmClassMappingKt.getKotlinClass(LoggingService.class));
            if(loggingService != null) {
                loggingService.upload(this, null, serviceResult -> {
                    logUploadPreference.setEnabled(true);
                    if (serviceResult instanceof ServiceResult.SUCCESS) {
                        Toast.makeText(requireActivity(), R.string.log_upload_ok, Toast.LENGTH_LONG).show();
                        LOGGER.info("Log is uploaded to the server.");
                    } else {
                        String message = ((ServiceResult.FAILURE) serviceResult).getMessage();
                        new DialogHelper(requireActivity())
                                .showOKOnlyDialog(
                                        requireActivity().getSupportFragmentManager(),
                                        message,
                                        null, null, null
                                );
                        LOGGER.error("Log upload failed with error message: " + message);
                    }
                });
            }
            return false;
        });

        changePasscodePreference = findPreference(getString(R.string.manage_passcode));
        changePasscodePreference.setOnPreferenceClickListener(preference -> {
            changePasscodePreference.setEnabled(false);
            FlowContext flowContext = new FlowContextBuilder(FlowContextRegistry.getFlowContext())
                    .setFlowType(FlowType.CHANGEPASSCODE)
                    .build();
            Flow.start(this.requireActivity(), flowContext, (requestCode, resultCode, data) -> {
                if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                    changePasscodePreference.setEnabled(true);
                }
                return null;
            });
            return false;
        });

        // Reset App
        Preference resetAppPreference = findPreference(getString(R.string.reset_app));
        resetAppPreference.setOnPreferenceClickListener(preference -> {
            FlowContext flowContext = new FlowContextBuilder(FlowContextRegistry.getFlowContext())
                .setFlowType(FlowType.RESET)
                .build();
            Flow.start(requireActivity(), flowContext, (requestCode, resultCode, data) -> {
                if (resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(requireActivity(), WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    requireActivity().startActivity(intent);
                }
                return null;
            });
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        prepareLogLevelSetting(logLevelPreference);
    }


    private void prepareLogLevelSetting(ListPreference listPreference) {
        Map<Level,String> mapping = getLevelStrings();
        logLevelPreference.setEntries(mapping.values().toArray(new String[0]));
        logLevelPreference.setEntryValues(getLevelValues());
        logLevelPreference.setPersistent(true);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
        String logString = sp.getString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, new LogPolicy().toString());
        LogPolicy settings = LogPolicy.createFromJsonString(logString);

        logLevelPreference.setSummary(mapping.get(LogPolicy.getLogLevel(settings)));
        logLevelPreference.setValue(String.valueOf(LogPolicy.getLogLevel(settings).levelInt));
        logLevelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // Get the new value
            Level logLevel = Level.toLevel(Integer.parseInt((String) newValue));
            LogPolicy newSettings = settings.copy(
                    settings.getEnabled(), settings.getMaxFileSize(),
                    LogPolicy.getLogLevelString(logLevel),
                    settings.getEntryExpiry(), settings.getMaxFileNumber()
            );
            sp.edit().putString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, newSettings.toString()).apply();
            LogPolicy.setRootLogLevel(newSettings);
            preference.setSummary(mapping.get(LogPolicy.getLogLevel(newSettings)));

            return true;
        });
    }

    private Map<Level, String> getLevelStrings() {
        Map<Level, String> mapping = new LinkedHashMap<>();
        mapping.put(Level.ALL, getString(R.string.log_level_path));
        mapping.put(Level.DEBUG, getString(R.string.log_level_debug));
        mapping.put(Level.INFO, getString(R.string.log_level_info));
        mapping.put(Level.WARN, getString(R.string.log_level_warning));
        mapping.put(Level.ERROR, getString(R.string.log_level_error));
        mapping.put(Level.OFF, getString(R.string.log_level_none));
        return mapping;
    }

    private String[] getLevelValues() {
        return new String[]{
                String.valueOf(Level.ALL.levelInt),
                String.valueOf(Level.DEBUG.levelInt),
                String.valueOf(Level.INFO.levelInt),
                String.valueOf(Level.WARN.levelInt),
                String.valueOf(Level.ERROR.levelInt),
                String.valueOf(Level.OFF.levelInt)};
    }
}
