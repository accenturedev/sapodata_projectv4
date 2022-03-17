package com.sap.appodatav4.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import com.sap.cloud.mobile.flowv2.ext.FlowOptions;
import com.sap.appodatav4.R;
import com.sap.cloud.mobile.fiori.onboarding.LaunchScreen;
import com.sap.cloud.mobile.fiori.onboarding.ext.LaunchScreenSettings;
import com.sap.cloud.mobile.flowv2.core.DialogHelper;
import com.sap.cloud.mobile.flowv2.core.Flow;
import com.sap.cloud.mobile.flowv2.core.FlowContext;
import com.sap.cloud.mobile.flowv2.core.FlowContextBuilder;

import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoader;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoaderCallback;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationPersistenceException;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationProvider;
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationProviderError;
import com.sap.cloud.mobile.foundation.configurationprovider.DefaultPersistenceMethod;
import com.sap.cloud.mobile.foundation.configurationprovider.FileConfigurationProvider;
import com.sap.cloud.mobile.foundation.configurationprovider.ProviderIdentifier;
import com.sap.cloud.mobile.foundation.configurationprovider.UserInputs;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WelcomeActivity extends AppCompatActivity {
    private FragmentManager fManager = this.getSupportFragmentManager();
    private Timer timer = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LaunchScreen welcome = new LaunchScreen(this);
        welcome.initialize(new LaunchScreenSettings.Builder()
                .setDemoButtonVisible(false)
                .setHeaderLineLabel(getString(R.string.welcome_screen_headline_label))
                .setPrimaryButtonText(getString(R.string.welcome_screen_primary_button_label))
                .setFooterVisible(true)
                .setUrlTermsOfService("http://www.sap.com")
                .setUrlPrivacy("http://www.sap.com")
                .addInfoViewSettings(
                        new LaunchScreenSettings.LaunchScreenInfoViewSettings(
                                R.drawable.ic_android_white_circle_24dp,
                                getString(R.string.application_name),
                                getString(R.string.welcome_screen_detail_label)
                        )
                )
                .build());
        welcome.setPrimaryButtonOnClickListener(v -> {
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> startConfigurationLoader());
                    }
                }, 400
            );
        });
        setContentView(welcome);
    }

    private void startConfigurationLoader(){
        Activity activity = this;
        ConfigurationLoaderCallback callback = new ConfigurationLoaderCallback() {
            @Override
            public void onCompletion(@Nullable ProviderIdentifier providerIdentifier, boolean success) {
                if (success) {
                    startFlow(activity);
                } else {
                    new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert)
                            .showOKOnlyDialog(
                                    fManager,
                                    getResources().getString(R.string.config_loader_complete_error_description),
                                    null, null, null
                            );
                }
            }

            @Override
            public void onError(@NonNull ConfigurationLoader configurationLoader, @NonNull ProviderIdentifier providerIdentifier, @NonNull UserInputs userInputs, @NonNull ConfigurationProviderError configurationProviderError) {
                new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert)
                        .showOKOnlyDialog(
                                fManager,
                                String.format(getResources().getString(
                                        R.string.config_loader_on_error_description),
                                        providerIdentifier.toString(), configurationProviderError.getErrorMessage()
                                ),
                                null, null, null
                        );
                configurationLoader.processRequestedInputs(new UserInputs());
            }

            @Override
            public void onInputRequired(@NonNull ConfigurationLoader configurationLoader, @NonNull UserInputs userInputs) {
                configurationLoader.processRequestedInputs(new UserInputs());
            }
        };
        ConfigurationProvider[] providers = {new FileConfigurationProvider(this, "sap_mobile_services")};
        this.runOnUiThread(() -> {
            ConfigurationLoader loader = new ConfigurationLoader(this, callback, providers);
            loader.loadConfiguration();
        });
    }

    public void startFlow(Activity context) {
        AppConfig appConfig = prepareAppConfig();
        if (appConfig == null) {
            return;
        }
        FlowContext flowContext = new FlowContextBuilder()
                .setApplication(appConfig)
                .setMultipleUserMode(false)
                .setFlowStateListener(new WizardFlowStateListener(
                        (SAPWizardApplication) context.getApplication()))
                .setFlowOptions(new FlowOptions() {
                    @Override
                    public boolean isDeleteRegistrationConfirmationNeeded() {
                        return false;
                    }
                })
                .build();
        Flow.start(context, flowContext, (requestCode, resultCode, data) -> {
            if (resultCode == RESULT_OK) {
                ((SAPWizardApplication)getApplication()).isApplicationUnlocked = true;
                Intent intent = new Intent(this, MainBusinessActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            return null;
        });
    }

    private AppConfig prepareAppConfig() {
        try {
            JSONObject configData = DefaultPersistenceMethod.getPersistedConfiguration(this);
            return AppConfig.createAppConfigFromJsonString(configData.toString());
        } catch (ConfigurationPersistenceException ex) {
            new DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            getResources().getString(R.string.config_data_build_json_description),
                            null, null, null
                    );
            return null;
        } catch (Exception ex) {
            new DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            ex.getLocalizedMessage(),
                            null, null, null
                    );
            return null;
        }
    }
}
