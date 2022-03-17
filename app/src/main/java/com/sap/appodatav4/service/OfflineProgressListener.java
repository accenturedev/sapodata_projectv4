package com.sap.appodatav4.service;

import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;
import com.sap.cloud.mobile.odata.offline.OfflineODataProviderOperationProgress;

import androidx.annotation.NonNull;

public abstract class OfflineProgressListener {
    public enum WorkerType {
        OPEN, SYNC
    }

    private int previousStep = 0;
    public final int totalStepsForTwoProgresses = 40;

    void onOfflineProgress(@NonNull OfflineODataProvider provider,
            @NonNull OfflineODataProviderOperationProgress progress) {
        if (progress.getCurrentStepNumber() > previousStep) {
            previousStep = progress.getCurrentStepNumber();
            if (getWorkerType() == WorkerType.OPEN && !OfflineWorkerUtil.userSwitchFlag) {
                updateProgress(progress.getCurrentStepNumber(), progress.getTotalNumberOfSteps());
            } else {
                /*
                 * The half of totalStepsForTwoProgresses is for first progress, the other half is for second progress.
                 * To make two progresses as one progress, the current step number needs to be calculated.
                 * For example, totalStepsForTwoProgresses is 40, then first progress will proceed from step 0 to step 20, and the second one will proceed from step 20 to step 40.
                 * So getStartPoint will be 0 for the first progress and 20 for the second progress.
                 * If first progress completes by 20% (i.e. getCurrentStepNumber / getTotalNumberOfSteps = 20%), the overall progress will be 4/40.
                 * If second progress completes by 20%, the overall progress will be 24/40.
                 */
                int currentStepNumber = totalStepsForTwoProgresses / 2 * progress.getCurrentStepNumber() / progress.getTotalNumberOfSteps() + getStartPoint();
                updateProgress(currentStepNumber, totalStepsForTwoProgresses);
            }
        }
        if (progress.getCurrentStepNumber() == progress.getTotalNumberOfSteps()) {
            previousStep = 0;
        }
    }

    public abstract void updateProgress(int currentStep, int totalSteps);
    public abstract int getStartPoint();
    public abstract WorkerType getWorkerType();
}
