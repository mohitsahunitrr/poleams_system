package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.WorkOrderType;

/**
 *
 * @author pchapman
 */
public final class WorkOrderTypes {
    
    private WorkOrderTypes() {}
    
    public static final WorkOrderType DistributionLineInspection = new WorkOrderType("DistributionLineInspection");
    public static final WorkOrderType TransmissionLineInspection = new WorkOrderType("TransmissionLineInspection");
    
}
