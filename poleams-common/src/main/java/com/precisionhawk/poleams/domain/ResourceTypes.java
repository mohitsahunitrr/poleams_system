package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.ResourceType;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author Philip A. Chapman
 */
public class ResourceTypes {
    
    // Do not instatiate
    private ResourceTypes() {}
    
    @Schema(description="Images taken by drone during inspection")
    public static final ResourceType DroneInspectionImage = new ResourceType("DroneInspectionImage");
    
    @Schema(description="LAS point cloud file per feeder")
    public static final ResourceType EncroachmentReport = new ResourceType("EncroachmentReport");
    
    @Schema(description="Google Earth shape (KLZ) file per feeder")
    public static final ResourceType EncroachmentShape = new ResourceType("EncroachmentShape");
    
    @Schema(description="Anomaly map (PDF) per feeder")
    public static final ResourceType FeederAnomalyMap = new ResourceType("FeederAnomalyMap");
    
    @Schema(description="Anomaly report (PDF) per feeder")
    public static final ResourceType FeederAnomalyReport = new ResourceType("FeederAnomalyReport");
    
    @Schema(description="Summary report (PDF) per feeder")
    public static final ResourceType FeederSummaryReport = new ResourceType("FeederSummaryReport");
    
    //TODO: Rename to "Circuit Map"
    @Schema(description="Map of feeder poles (PDF) per feeder")
    public static final ResourceType FeederMap = new ResourceType("FeederMap");
    
    @Schema(description="Images taken manually during inspection")
    public static final ResourceType ManualInspectionImage = new ResourceType("ManualInspectionImage");
    
    @Schema(description="Misc. files gathered during inspeciton")
    public static final ResourceType Other = new ResourceType("Other");
    
    @Schema(description="Design report (PDF) per pole")
    public static final ResourceType PoleDesignReport = new ResourceType("PoleDesignReport");
    
    //TODO: Rename to "PoleForemanAnalysisXML"
    @Schema(description="Pole Foreman analysis data")
    public static final ResourceType PoleInspectionAnalysisXML = new ResourceType("PoleInspectionAnalysisXML");
    
    @Schema(description="PDF Output from Pole Forman per pole")
    public static final ResourceType PoleInspectionReport = new ResourceType("PoleInspectionReport");
    
    @Schema(description="Excel Spreadsheet per feeder")
    public static final ResourceType SurveyReport = new ResourceType("SurveyReport");
    
    @Schema(description="Thermal images taken by drone during inspection")
    public static final ResourceType Thermal = new ResourceType("Thermal");
    
    @Schema(description="Thumbnails of the Drone Inspection Images")
    public static final ResourceType ThumbNail = new ResourceType("ThumbNail");
    
    private static final ResourceType[] types = {
        DroneInspectionImage, EncroachmentReport, EncroachmentShape,
        FeederAnomalyMap, FeederAnomalyReport, FeederSummaryReport,
        FeederMap, ManualInspectionImage, Other, PoleDesignReport,
        PoleInspectionAnalysisXML, PoleInspectionReport, SurveyReport,
        Thermal, ThumbNail
    };
    
    public static ResourceType valueOf(String value) {
        if (value != null) {
            for (ResourceType t : values()) {
                if (value.equals(t.getValue())) {
                    return t;
                }
            }
        }
        return null;
    }

    public static ResourceType[] values() {
        return types;
    }
}
