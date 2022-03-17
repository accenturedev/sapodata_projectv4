package com.sap.appodatav4.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.sap.appodatav4.R;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.MenuItem;
import android.view.View;
import android.app.NotificationManager;
import androidx.appcompat.widget.Toolbar;
import androidx.work.WorkManager;

import com.sap.appodatav4.mdui.SelectApp;
import com.sap.cloud.mobile.flowv2.core.DialogHelper;
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry;
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate;
import com.sap.cloud.mobile.odata.offline.OfflineODataException;
import com.sap.cloud.mobile.fiori.onboarding.OfflineInitSyncScreen;
import com.sap.cloud.mobile.fiori.onboarding.OfflineNetworkErrorScreen;
import com.sap.cloud.mobile.fiori.onboarding.OfflineTransactionIssueScreen;
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineNetworkErrorScreenSettings;
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineTransactionIssueScreenSettings;
import com.sap.cloud.mobile.foundation.user.DeviceUser;
import com.sap.cloud.mobile.foundation.model.AppConfig;
import kotlin.Unit;
import com.sap.appodatav4.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBusinessActivity extends AppCompatActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainBusinessActivity.class);
    private DialogFragment dialogFragment = null;
    private boolean isOfflineStoreInitialized = false;
    private OfflineInitSyncScreen offlineInitSyncScreen = null;

    private final OfflineProgressListener progressListener = new OfflineProgressListener() {
        @Override
        public void updateProgress(int currentStep,
                int totalSteps) {
            offlineInitSyncScreen.updateProgressBar(currentStep, totalSteps);
        }

        @Override
        public int getStartPoint() {
            return OfflineOpenWorker.getStartPointForOpen();
        }

        @Override
        public WorkerType getWorkerType() {
            return WorkerType.OPEN;
        }
    };

    private void navigateToEntityList() {
        Intent intent = new Intent(this, SelectApp.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_business);
        dialogFragment = new DialogHelper.ErrorDialogFragment(
                getString(R.string.offline_navigation_dialog_message),
                R.style.Flows_Dialog,
                getString(R.string.offline_navigation_dialog_title),
                getString(R.string.offline_navigation_dialog_positive_option),
                getString(R.string.offline_navigation_dialog_negative_option),
                (() -> {
                    if (!isOfflineStoreInitialized) {
                        getApplication().getSystemService(NotificationManager.class).cancel(OfflineBaseWorker.OFFLINE_NOTIFICATION_CHANNEL_INT_ID);
                        WorkManager.getInstance(getApplication()).cancelUniqueWork(OfflineWorkerUtil.OFFLINE_OPEN_WORKER_UNIQUE_NAME);
                    }
                    backToWelcome();
                    return Unit.INSTANCE;
                }),
                null
        );
        dialogFragment.setCancelable(false);
    }

    private void startEntitySetListActivity() {
        if (isOfflineStoreInitialized) {
            OfflineWorkerUtil.resetOpenRequest();
            navigateToEntityList();
        } else {
            LOGGER.info("Waiting for the sync finish.");
            WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(OfflineWorkerUtil.getOpenRequest().getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        OfflineWorkerUtil.removeProgressListener(progressListener);
                        OfflineWorkerUtil.resetOpenRequest();
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                navigateToEntityList();
                                break;
                            case FAILED:
                                getSupportActionBar().setTitle(R.string.initializing_offline_store_failed);
                                switch (workInfo.getOutputData().getInt(OfflineWorkerUtil.OUTPUT_ERROR_KEY, 0)) {
                                    case -1:
                                        offlineNetworkErrorAction();
                                        break;
                                    case -10425:
                                        offlineTransactionIssueAction();
                                        break;
                                    default:
                                        new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert)
                                                .showOKOnlyDialog(
                                                        getSupportFragmentManager(),
                                                        (workInfo.getOutputData().getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) == null)? "Offline sync failed" : workInfo.getOutputData().getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL),
                                                        getResources().getString(R.string.offline_initial_open_error), null,
                                                        (() -> {
                                                            Intent intent = new Intent(this, WelcomeActivity.class);
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent);
                                                            return Unit.INSTANCE;
                                                        })
                                                );
                                        break;
                                }
                                break;
                        }
                    }
                });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toolbar toolbar = findViewById(R.id.toolbar);
        offlineInitSyncScreen = findViewById(R.id.offlineInitSyncScreen);
        findViewById(R.id.offlineNetworkErrorScreen).setVisibility(View.INVISIBLE);
        findViewById(R.id.offlineTransactionIssueScreen).setVisibility(View.INVISIBLE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.initializing_offline_store);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isOfflineStoreInitialized = sharedPreferences.getBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false);

        if (isOfflineStoreInitialized) {
            offlineInitSyncScreen.setVisibility(View.INVISIBLE);
            findViewById(R.id.main_bus_resume_progress_bar).setVisibility(View.VISIBLE);
        } else {
            OfflineWorkerUtil.addProgressListener(progressListener);
            offlineInitSyncScreen.setVisibility(View.VISIBLE);
            findViewById(R.id.main_bus_resume_progress_bar).setVisibility(View.INVISIBLE);
        }
        boolean isMultipleUserMode =
                UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() != null
                        && UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync();

        AppConfig appConfig = FlowContextRegistry.getFlowContext().getAppConfig();

        OfflineWorkerUtil.initializeOffline(getApplication(), appConfig, isMultipleUserMode);
        OfflineWorkerUtil.open(getApplication());
        startEntitySetListActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (OfflineWorkerUtil.getOpenRequest() != null) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, true)
                    .apply();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (findViewById(R.id.offlineNetworkErrorScreen).getVisibility() == View.VISIBLE
                        || findViewById(R.id.offlineTransactionIssueScreen).getVisibility() == View.VISIBLE) {
                    backToWelcome();
                } else {
                    dialogFragment.show(getSupportFragmentManager(), "Back");
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.offlineNetworkErrorScreen).getVisibility() == View.VISIBLE
                || findViewById(R.id.offlineTransactionIssueScreen).getVisibility() == View.VISIBLE) {
            backToWelcome();
        } else {
            dialogFragment.show(getSupportFragmentManager(), "Back");
        }
    }

    private void backToWelcome() {
        OfflineWorkerUtil.removeProgressListener(progressListener);
        OfflineWorkerUtil.resetOpenRequest();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void offlineNetworkErrorAction() {
        runOnUiThread(() -> {
            OfflineNetworkErrorScreen offlineNetworkErrorScreen = findViewById(R.id.offlineNetworkErrorScreen);
            offlineNetworkErrorScreen.setVisibility(View.VISIBLE);
            offlineInitSyncScreen.setVisibility(View.INVISIBLE);
            findViewById(R.id.main_bus_resume_progress_bar).setVisibility(View.INVISIBLE);

            OfflineNetworkErrorScreenSettings offlineNetworkErrorScreenSettings = new OfflineNetworkErrorScreenSettings.Builder().build();

            offlineNetworkErrorScreen.initialize(offlineNetworkErrorScreenSettings);
            offlineNetworkErrorScreen.setButtonClickListener(
                    v -> {
                        OfflineWorkerUtil.addProgressListener(progressListener);
                        OfflineWorkerUtil.open(getApplication());
                        startEntitySetListActivity();
                        getSupportActionBar().setTitle(R.string.initializing_offline_store);
                        offlineNetworkErrorScreen.setVisibility(View.INVISIBLE);
                        offlineInitSyncScreen.setVisibility(View.VISIBLE);
                    }
            );
        });
    }

    private void offlineTransactionIssueAction() {
        runOnUiThread(() -> {
            OfflineTransactionIssueScreen offlineTransactionIssueScreen = findViewById(R.id.offlineTransactionIssueScreen);
            offlineTransactionIssueScreen.setVisibility(View.VISIBLE);
            offlineInitSyncScreen.setVisibility(View.INVISIBLE);
            findViewById(R.id.main_bus_resume_progress_bar).setVisibility(View.INVISIBLE);
            String previousUser = null;
            try {
                previousUser = OfflineWorkerUtil.getOfflineODataProvider().getPreviousUser();
            } catch (OfflineODataException exception) {
                LOGGER.error("Cannot get info of previous user. Exception happens: " + exception.getMessage());
            }

            OfflineTransactionIssueScreenSettings offlineTransactionIssueScreenSettings = new OfflineTransactionIssueScreenSettings.Builder().build();

            offlineTransactionIssueScreen.initialize(offlineTransactionIssueScreenSettings);
            if (previousUser != null) {
                DeviceUser user = UserSecureStoreDelegate.getInstance().getUserInfoByIdAsync(previousUser);
                if (user != null) {
                    offlineTransactionIssueScreen.setPrevUserName(user.getName());
                    offlineTransactionIssueScreen.setPrevUserMail(user.getEmail());
                } else {
                    offlineTransactionIssueScreen.setPrevUserName(previousUser);
                }
            }
            offlineTransactionIssueScreen.setButtonClickListener(
                    v -> {
                        Intent transactionIssueHandleIntent = new Intent(this, WelcomeActivity.class);
                        transactionIssueHandleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(transactionIssueHandleIntent);
                    }
            );
        });
    }
}
