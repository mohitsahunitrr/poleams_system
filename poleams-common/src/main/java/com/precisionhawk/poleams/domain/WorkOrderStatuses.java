package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.WorkOrderStatus;

/**
 *
 * @author pchapman
 */
public final class WorkOrderStatuses {
    
    private WorkOrderStatuses() {}

    public static final WorkOrderStatus Requested = new WorkOrderStatus("Requested");
    public static final WorkOrderStatus Onsite = new WorkOrderStatus("Onsite");
    public static final WorkOrderStatus ImagesUploaded = new WorkOrderStatus("ImagesUploaded");
    public static final WorkOrderStatus ImagesProcessed = new WorkOrderStatus("ImagesProcessed");
    public static final WorkOrderStatus Completed = new WorkOrderStatus("Completed");
}
