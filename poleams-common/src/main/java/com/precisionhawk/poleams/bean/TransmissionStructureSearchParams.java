package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.AssetSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="Search parameters for transmission structures.")
public class TransmissionStructureSearchParams extends AssetSearchParams {
    @Schema(description="The unique number of the structure assigned by the utility.")
    private String structureNumber;
    public String getStructureNumber() {
        return structureNumber;
    }
    public void setStructureNumber(String structureNumber) {
        this.structureNumber = structureNumber;
    }

    @Override
    public boolean hasCriteria() {
        return super.hasCriteria() || hasValue(structureNumber);
    }
    
}
