package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.AssetSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 * Parameters for searching for poles in the data store.
 *
 * @author Philip A. Chapman
 */
@Schema(description="Parameters for searching for poles in the data store.")
public class PoleSearchParams extends AssetSearchParams {
    
    @Schema(description="Unique ID of the pole assigned by the Utility.  Only one result should be returned.")
    private String utilityId;
    public String getUtilityId() {
        return utilityId;
    }
    public void setUtilityId(String id) {
        this.utilityId = id;
    }
    
    @Override
    public boolean hasCriteria() {
        return super.hasCriteria() || hasValue(utilityId);
    }
}
