package com.precisionhawk.poleams.processors.poleinspection;

/**
 *
 * @author Philip A. Chapman
 */
public enum ImportProcessStatus {
    Initializing,
    GeneratingUpdatedSurveyReport,
    ProcessingMasterSurveyTemplate,
    ProcessingPoleData,
    PersistingData,
    UploadingResources,
    Done
}
