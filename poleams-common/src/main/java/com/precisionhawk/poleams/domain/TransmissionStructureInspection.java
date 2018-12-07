package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.AssetInspection;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="An inspection of a transmission structure.")
public class TransmissionStructureInspection extends AssetInspection {
    
    @Schema(description="The reason a structure was not inspected.")
    private String reasonNotInspected;
    public String getReasonNotInspected() {
        return reasonNotInspected;
    }
    public void setReasonNotInspected(String reasonNotInspected) {
        this.reasonNotInspected = reasonNotInspected;
    }
    
}
