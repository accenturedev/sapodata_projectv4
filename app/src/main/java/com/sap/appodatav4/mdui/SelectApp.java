package com.sap.appodatav4.mdui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.sap.appodatav4.R;
import com.sap.appodatav4.mdui.buttonselect.ButtonSelect;
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectApp extends AppCompatActivity {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectApp.class);
    private FioriProgressBar progressBar = null;
    private MenuItem syncItem = null;
    private Context context = SelectApp.this;

    // string for example
    private String[] data = {"Ordini", "Immessi", "Movimenti", "Inventari", "Anagrafiche e prezzi", "Assistenza"};
    private String[] data2 = {"Riordino", "Promemoria", "Storico Ordini"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_app);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.sync_determinate);


        /*
        final CardView cardView1 = findViewById(R.id.cardView);
        final CardView cardView2 = findViewById(R.id.cardView2);


        String[] data = {"Ordini", "Immessi", "Movimenti", "Inventari", "Anagrafiche e prezzi", "Assistenza"};

        String[] data2 = {"Riordino", "Promemoria", "Storico Ordini"};



        cardView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                intent = new Intent(context, ButtonSelect.class);

                intent.putExtra("data", data);
                context.startActivity(intent);
            }
        });
        cardView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                intent = new Intent(context, ButtonSelect.class);
                intent.putExtra("data", data2);
                context.startActivity(intent);
            }
        });

         */
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    public void buttonClicked(View view) {

        if (view.getId() == R.id.cardView) {
            Intent intent;
            intent = new Intent(this, ButtonSelect.class);

            intent.putExtra("data", data);
            startActivity(intent);
        } else if (view.getId() == R.id.cardView2) {
            Intent intent;
            intent = new Intent(this, ButtonSelect.class);

            intent.putExtra("data", data2);
            startActivity(intent);
        }

    }
    /* Code for menu and Sync
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.entity_set_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_delete_registration).setEnabled(
                UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() != null
                        && UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync());
        menu.findItem(R.id.menu_delete_registration).setVisible(
                UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() != null
                        && UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync());
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGGER.debug("onOptionsItemSelected: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.menu_settings:
                LOGGER.debug("settings screen menu item selected.");
                this.startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_sync:
                syncItem = item;
                synchronize();
                return true;

            case R.id.menu_logout:
                FlowContext flowContext_logout = new FlowContextBuilder(FlowContextRegistry.getFlowContext())
                        .setFlowType(FlowType.LOGOUT)
                        .build();
                Flow.start(this, flowContext_logout, (requestCode, resultCode, data) -> {
                    if (resultCode == RESULT_OK) {

                        Intent intent = new Intent(this, WelcomeActivity.class);
                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    return null;
                });
                return true;

            case R.id.menu_delete_registration:
                DialogFragment dialogFragment = new DialogHelper.ErrorDialogFragment(
                        getString(R.string.delete_registration_warning),
                        R.style.Flows_Dialog,
                        getString(R.string.dialog_warn_title),
                        getString(R.string.confirm_yes),
                        getString(R.string.cancel),
                        () -> {
                            FlowContext flowContext_del_reg = new FlowContextBuilder(FlowContextRegistry.getFlowContext())
                                    .setFlowType(FlowType.DEL_REGISTRATION)
                                    .build();
                            Flow.start(this, flowContext_del_reg, (requestCode, resultCode, data) -> {
                                if (resultCode == RESULT_OK) {
                                    PreferenceManager.getDefaultSharedPreferences(this)
                                            .edit()
                                            .putBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, true)
                                            .apply();
                                    Intent intent = new Intent(this, WelcomeActivity.class);
                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                                return null;
                            });
                            return Unit.INSTANCE;
                        },
                        null
                );
                dialogFragment.setCancelable(false);
                dialogFragment.show(getSupportFragmentManager(), getString(R.string.delete_registration));
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OfflineWorkerUtil.getSyncRequest() != null) {
            updateProgressForSync();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (OfflineWorkerUtil.getSyncRequest() != null) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, true)
                    .apply();
        }
    }

    private void synchronize() {
        OfflineWorkerUtil.sync(getApplicationContext());
        updateProgressForSync();
    }

    private void updateProgressForSync() {
        if (syncItem != null) {
            syncItem.setEnabled(false);
        }
        OfflineWorkerUtil.addProgressListener(progressListener);
        progressBar.setVisibility(View.VISIBLE);
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(OfflineWorkerUtil.getSyncRequest().getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (syncItem != null) {
                            syncItem.setEnabled(true);
                        }
                        OfflineWorkerUtil.removeProgressListener(progressListener);
                        OfflineWorkerUtil.resetSyncRequest();
                        progressBar.setVisibility(View.INVISIBLE);
                        progressBar.setProgress(0);
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                LOGGER.info("Offline sync done.");
                                break;
                            case FAILED:
                                new DialogHelper(getApplication(), R.style.OnboardingDefaultTheme_Dialog_Alert).showOKOnlyDialog(
                                        getSupportFragmentManager(),
                                        (workInfo.getOutputData().getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) == null)? getString(R.string.synchronize_failure_detail) : workInfo.getOutputData().getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL),
                                        null, null, null
                                );
                                break;
                        }
                    }
                });
    }

    private final OfflineProgressListener progressListener = new OfflineProgressListener() {
        @Override
        public void updateProgress(int currentStep,
                                   int totalSteps) {
            progressBar.setMax(totalSteps);
            progressBar.setProgress(currentStep);
        }

        @Override
        public int getStartPoint() {
            return OfflineSyncWorker.getStartPointForSync();
        }

        @Override
        public WorkerType getWorkerType() {
            return WorkerType.SYNC;
        }
    };
*/
}

