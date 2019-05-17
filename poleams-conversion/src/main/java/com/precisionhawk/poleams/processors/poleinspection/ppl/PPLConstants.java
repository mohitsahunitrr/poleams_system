package com.precisionhawk.poleams.processors.poleinspection.ppl;

import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.WorkOrderStatus;

/**
 *
 * @author pchapman
 */
public interface PPLConstants {
    static final String ORG_ID = "835b679b-cbb0-4314-a355-f3ff7a9c575c";
    
    public static final AssetInspectionStatus AI_PENDING = new AssetInspectionStatus("Pending"); // Black               Not yet collected or processed
    public static final AssetInspectionStatus AI_PROCESSED = new AssetInspectionStatus("Processed"); // Yellow          Data collected, not yet processed
    
    public static final WorkOrderStatus WO_PENDING = new WorkOrderStatus("Pending");
    public static final WorkOrderStatus WO_PROCESSED = new WorkOrderStatus("Processed");
    
    public static final SiteInspectionStatus SI_PENDING = new SiteInspectionStatus("Pending");
    public static final SiteInspectionStatus SI_PROCESSED = new SiteInspectionStatus("Processed");
}
