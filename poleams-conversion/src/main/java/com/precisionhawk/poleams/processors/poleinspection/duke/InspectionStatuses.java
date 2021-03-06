package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.WorkOrderStatus;

/**
 *
 * @author pchapman
 */
//TODO: This needs to be done differently
public interface InspectionStatuses {
    public static final AssetInspectionStatus AI_PENDING = new AssetInspectionStatus("Pending"); // Black               Not yet collected or processed
    public static final AssetInspectionStatus AI_PROCESSED = new AssetInspectionStatus("Processed"); // Yellow          Data collected, not yet processed
    public static final AssetInspectionStatus AI_PENDING_MERGE = new AssetInspectionStatus("PendingMerge"); // Orange   Data collected and processed, differences identified
    public static final AssetInspectionStatus AI_COMPLETE = new AssetInspectionStatus("Complete"); // Green             Data collected and processed, no differences identified
    
    public static final WorkOrderStatus WO_PENDING = new WorkOrderStatus("Pending");
    public static final WorkOrderStatus WO_PROCESSED = new WorkOrderStatus("Processed");
    
    public static final SiteInspectionStatus SI_PENDING = new SiteInspectionStatus("Pending"); // Black
    public static final SiteInspectionStatus SI_PROCESSED = new SiteInspectionStatus("Processed"); // Yellow
    public static final SiteInspectionStatus SI_PENDING_MERGE = new SiteInspectionStatus("PendingMerge"); // Orange
    public static final SiteInspectionStatus SI_COMPLETE = new SiteInspectionStatus("Complete"); // Green
}
