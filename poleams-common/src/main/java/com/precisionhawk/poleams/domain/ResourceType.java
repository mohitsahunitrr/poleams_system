package com.precisionhawk.poleams.domain;

import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author Philip A. Chapman
 */
public enum ResourceType {
    
    @Schema(description="Images taken by drone during inspection")
    DroneInspectionImage,
    
    @Schema(description="LAS point cloud file per feeder")
    EncroachmentReport,
    
    @Schema(description="Google Earth shape (KLZ) file per feeder")
    EncroachmentShape,
    
    @Schema(description="Anomaly map (PDF) per feeder")
    FeederAnomalyMap,
    
    @Schema(description="Anomaly report (PDF) per feeder")
    FeederAnomalyReport,
    
    @Schema(description="Summary report (PDF) per feeder")
    FeederSummaryReport,
    
    //TODO: Rename to "Circuit Map"
    @Schema(description="Map of feeder poles (PDF) per feeder")
    FeederMap,
    
    @Schema(description="Images taken manually during inspection")
    ManualInspectionImage,
    
    @Schema(description="Misc. files gathered during inspeciton")
    Other,
    
    //TODO: Rename to "PoleForemanAnalysisXML"
    @Schema(description="Pole Foreman analysis data")
    PoleInspectionAnalysisXML,
    
    @Schema(description="PDF Output from Pole Forman per pole")
    PoleInspectionReport,
    
    @Schema(description="Excel Spreadsheet per feeder")
    SurveyReport,
    
    @Schema(description="Thermal images taken by drone during inspection")
    Thermal,
    
    @Schema(description="Thumbnails of the Drone Inspection Images")
    ThumbNail
    
}
