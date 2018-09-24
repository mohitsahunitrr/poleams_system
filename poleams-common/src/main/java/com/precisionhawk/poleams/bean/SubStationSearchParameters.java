package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 * Parameters for searching for sub stations in the data store.
 *
 * @author Philip A. Chapman
 */
@Schema(description="Parameters for searching for sub stations in the data store.")
public class SubStationSearchParameters {

    @Schema(description="User friendly name for the substation.")
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    @Schema(description="Unique ID associated with the feeder coming into the substation.")
    private String feederNumber;
    public String getFeederNumber() {
        return feederNumber;
    }
    public void setFeederNumber(String feederNumber) {
        this.feederNumber = feederNumber;
    }
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
