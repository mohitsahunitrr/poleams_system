package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.SiteSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 * Parameters for searching for sub stations in the data store.
 *
 * @author Philip A. Chapman
 */
@Schema(description="Parameters for searching for sub stations in the data store.")
public class FeederSearchParams extends SiteSearchParams {
    
    public FeederSearchParams() {}
    
    public FeederSearchParams(SiteSearchParams params) {
        setName(params.getName());
        setOrganizationId(params.getOrganizationId());
    }

    @Schema(description="Unique ID associated with the feeder coming into the substation.")
    private String feederNumber;
    public String getFeederNumber() {
        return feederNumber;
    }
    public void setFeederNumber(String feederNumber) {
        this.feederNumber = feederNumber;
    }
    
    @Override
    public boolean hasCriteria() {
        return super.hasCriteria() || hasValue(feederNumber);
    }
}
