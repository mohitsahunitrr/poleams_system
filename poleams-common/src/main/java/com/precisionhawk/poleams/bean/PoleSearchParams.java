package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.AbstractSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 * Parameters for searching for poles in the data store.
 *
 * @author Philip A. Chapman
 */
@Schema(description="Parameters for searching for poles in the data store.")
public class PoleSearchParams extends AbstractSearchParams {
    
    @Schema(description="Unique ID of the pole assigned by the Utility.  Only one result should be returned.")
    private String utilityId;
    public String getUtilityId() {
        return utilityId;
    }
    public void setUtilityId(String id) {
        this.utilityId = id;
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
    private String siteId;
    public String getSiteId() {
        return siteId;
    }
    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }    

    @Override
    public boolean hasCriteria() {
        return testField(utilityId) || testField(organizationId) || testField(siteId);
    }
}
