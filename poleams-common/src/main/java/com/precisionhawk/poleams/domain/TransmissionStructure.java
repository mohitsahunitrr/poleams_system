package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.Asset;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="A structure (tower) that offers support to a transmission line.")
public class TransmissionStructure extends Asset {
    
    @Schema(description="The unique number of the structure assigned by the utility.")
    private String structureNumber;
    public String getStructureNumber() {
        return structureNumber;
    }
    public void setStructureNumber(String structureNumber) {
        this.structureNumber = structureNumber;
    }
}
