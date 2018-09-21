/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Schema(description="A bean containing search criteria for resources and objects related to resources.  At least one field must have a non-null value.")
public class ResourceSearchParameters {
    
    @Schema(description="The organization.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Schema(description="The unique ID of the related pole.")
    private String poleId;
    public String getPoleId() {
        return poleId;
    }
    public void setPoleId(String poleId) {
        this.poleId = poleId;
    }

    @Schema(description="The unique ID of the related pole inspection.")
    private String poleInspectionId;
    public String getPoleInspectionId() {
        return poleInspectionId;
    }
    public void setPoleInspectionId(String poleInspectionId) {
        this.poleInspectionId = poleInspectionId;
    }

    /**
     * Unique ID of the original resource image if this one is the result of
     * processing.
     */
    @Schema(description="If this resource was produced by modifying another resource (such as cropping an image), this is the unique ID of the source resource.")
    private String sourceResourceId;
    public String getSourceResourceId() {
        return sourceResourceId;
    }
    public void setSourceResourceId(String id) {
        this.sourceResourceId = id;
    }

    @Schema(description="The status of the resource.")
    private ResourceStatus status;
    public ResourceStatus getStatus() {
	return status;
    }
    public void setStatus(ResourceStatus status) {
	this.status = status;
    }

    @Schema(description="The unique ID of the substation to which this pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }

    @Schema(description="The type of resource being stored.")
    private ResourceType type;
    public ResourceType getType() {
        return type;
    }
    public void setType(ResourceType type) {
        this.type = type;
    }
    
    @Schema(description="The unique ID of the zoomify data for the image.")
    private String zoomifyId;
    public String getZoomifyId() {
        return zoomifyId;
    }
    public void setZoomifyId(String zoomifyId) {
        this.zoomifyId = zoomifyId;
    }
    
    public boolean hasCriteria() {
        return testField(subStationId) || testField(organizationId) || testField(poleId) ||
                testField(poleInspectionId) || status != null || testField(sourceResourceId)
                || type != null;
    }
    
    private boolean testField(String field) {
        return field != null && field.length() > 0;
    }
    
    public ResourceSearchParameters() {}
}
