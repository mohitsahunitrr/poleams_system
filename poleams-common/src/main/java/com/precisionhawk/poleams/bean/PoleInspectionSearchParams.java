package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.AbstractSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 * Search parameters for querying for pole inspections.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="Search parameters for querying for pole inspections.")
public class PoleInspectionSearchParams extends AbstractSearchParams {
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Schema(description="Unqiue ID of the pole that was inspected.")
    private String assetId;
    public String getAssetId() {
        return assetId;
    }
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    @Schema(description="The feeder to which the inspected pole is related.")
    private String siteId;
    public String getSiteId() {
        return siteId;
    }
    public void setSiteId(String subSiteId) {
        this.siteId = subSiteId;
    }    

    @Override
    public boolean hasCriteria() {
        return testField(organizationId) || testField(assetId) || testField(siteId);
    }
}
