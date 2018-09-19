package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 * Search parameters for querying for pole inspections.
 * 
 * @author pchapman
 */
@Schema(description="Search parameters for querying for pole inspections.")
public class PoleInspectionSearchParameters {
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Schema(description="Unqiue ID of the pole that was inspected.")
    private String poleId;
    public String getPoleId() {
        return poleId;
    }
    public void setPoleId(String poleId) {
        this.poleId = poleId;
    }

    @Schema(description="The substation to which the inspected pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }    
}
