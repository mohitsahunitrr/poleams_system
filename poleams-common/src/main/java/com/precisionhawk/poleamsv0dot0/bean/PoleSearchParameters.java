package com.precisionhawk.poleamsv0dot0.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 * Parameters for searching for poles in the data store.
 *
 * @author Philip A. Chapman
 */
@Schema(description="Parameters for searching for poles in the data store.")
public class PoleSearchParameters {
    
    @Schema(description="Unique ID of the pole assigned by the Utility.  Only one result should be returned.")
    private String fplId;
    public String getFPLId() {
        return fplId;
    }
    public void setFPLId(String id) {
        this.fplId = id;
    }
    
    @Schema(description="The organization to which the poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    @Schema(description="The unique ID of the substation to which the poles are related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }    
}
