package com.sap.appodatav4.mdui;

import com.sap.appodatav4.app.SAPWizardApplication;

import com.sap.cloud.mobile.flowv2.core.DialogHelper;
import com.sap.cloud.mobile.flowv2.core.Flow;
import com.sap.cloud.mobile.flowv2.core.FlowContext;
import com.sap.cloud.mobile.flowv2.core.FlowContextBuilder;
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry;
import com.sap.cloud.mobile.flowv2.model.FlowType;
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;
import com.sap.appodatav4.service.OfflineProgressListener;
import com.sap.appodatav4.service.OfflineSyncWorker;
import com.sap.appodatav4.service.OfflineWorkerUtil;
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar;
import android.os.Bundle;
import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kotlin.Unit;
import com.sap.appodatav4.app.WelcomeActivity;
import com.sap.appodatav4.mdui.orderitemset.OrderItemSetActivity;
import com.sap.appodatav4.mdui.orderset.OrderSetActivity;
import com.sap.cloud.mobile.fiori.object.ObjectCell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.appodatav4.R;

/*
 * An activity to display the list of all entity types from the OData service
 */
public class EntitySetListActivity extends AppCompatActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySetListActivity.class);
    private static final int BLUE_ANDROID_ICON = R.drawable.ic_android_blue;
    private static final int WHITE_ANDROID_ICON = R.drawable.ic_android_white;

    public enum EntitySetName {
        OrderItemSet("OrderItemSet", R.string.eset_orderitemset,BLUE_ANDROID_ICON),
        OrderSet("OrderSet", R.string.eset_orderset,WHITE_ANDROID_ICON);

        private final int titleId;
        private final int iconId;
        private final String entitySetName;

        EntitySetName(String name, int titleId, int iconId) {
            this.entitySetName = name;
            this.titleId = titleId;
            this.iconId = iconId;
        }

        public int getTitleId() {
                return this.titleId;
        }

        public String getEntitySetName() {
                return this.entitySetName;
        }
    }

    private final List<String> entitySetNames = new ArrayList<>();
    private final Map<String, EntitySetName> entitySetNameMap = new HashMap<>();

    /* Fiori progress bar for busy indication if either update or delete action is clicked upon */
    private FioriProgressBar progressBar = null;
    private MenuItem syncItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_entity_set_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.sync_determinate);

        entitySetNames.clear();
        entitySetNameMap.clear();
        for (EntitySetName entitySet : EntitySetName.values()) {
            String entitySetTitle = getResources().getString(entitySet.getTitleId());
            entitySetNames.add(entitySetTitle);
            entitySetNameMap.put(entitySetTitle, entitySet);
        }

        final ListView listView = findViewById(R.id.entity_list);
        final EntitySetListAdapter adapter = new EntitySetListAdapter(this, R.layout.element_entity_set_list, entitySetNames);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            EntitySetName entitySetName = entitySetNameMap.get(adapter.getItem(position));
            Context context = EntitySetListActivity.this;
            Intent intent;
            switch (entitySetName) {
                case OrderItemSet:
                    intent = new Intent(context, OrderItemSetActivity.class);
                    break;
                case OrderSet:
                    intent = new Intent(context, OrderSetActivity.class);
                    break;
                    default:
                        return;
            }
            context.startActivity(intent);
        });
    }

    public class EntitySetListAdapter extends ArrayAdapter<String> {

        EntitySetListAdapter(@NonNull Context context, int resource, List<String> entitySetNames) {
            super(context, resource, entitySetNames);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            EntitySetName entitySetName = entitySetNameMap.get(getItem(position));
            if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.element_entity_set_list, parent, false);
            }
            String headLineName = getResources().getString(entitySetName.titleId);

            ObjectCell entitySetCell = convertView.findViewById(R.id.entity_set_name);
            entitySetCell.setHeadline(headLineName);
            entitySetCell.setDetailImage(entitySetName.iconId);
            return convertView;
        }
    }
    /*
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, SelectApp.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();

    }

     */
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

}
