package com.precisionhawk.poleams.domain;

import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
public class InspectionEvent extends com.precisionhawk.ams.domain.InspectionEvent {
    
    @Schema(description="The position of the identified damage on the asset.")
    private String position;
    public String getPosition() {
        return position;
    }
    public void setPosition(String position) {
        this.position = position;
    }
}
