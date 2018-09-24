package com.precisionhawk.poleams.domain;

import io.swagger.oas.annotations.media.Schema;

/**
 * A power substation.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="Data related to power substation to which poles are related.")
public class SubStation implements Identifyable {
    
    @Schema(description="Unique ID of the substation.")
    private String id;
    @Override
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

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
    
    @Schema(description="The hardening level for the substation.  e.g. \"Extreme Wind\"")
    private String hardeningLevel;
    public String getHardeningLevel() {
        return hardeningLevel;
    }
    public void setHardeningLevel(String hardeningLevel) {
        this.hardeningLevel = hardeningLevel;
    }
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    @Schema(description="The wind zone that the substation is located in, e.g. 145 = rated for up to 145 MPH winds.")
    private String windZone;
    public String getWindZone() {
        return windZone;
    }
    public void setWindZone(String windZone) {
        this.windZone = windZone;
    }
}
