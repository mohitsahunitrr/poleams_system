package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.Pole;
import io.swagger.oas.annotations.media.Schema;

/**
 * A displayable summary of Pole information.
 *
 * @author Philip A. Chapman
 */
@Schema(description="A displayable summary of Pole information.")
public class PoleSummary extends Pole {

    @Schema(description="The type of equipment on the pole.  (Documents the primary equipment since a variety may exist.)")
    private String equipmentType;
    public String getEquipmentType() {
        return equipmentType;
    }
    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    @Schema(description="The framing of spans on the pole.  (Documents the primary since other spans may have different types of framing.)")
    private String framing;
    public String getFraming() {
        return framing;
    }
    public void setFraming(String framing) {
        this.framing = framing;
    }

    @Schema(description="The number of phases of the primary span on the pole.)")
    private Integer numberOfPhases;
    public Integer getNumberOfPhases() {
        return numberOfPhases;
    }
    public void setNumberOfPhases(Integer numberOfPhases) {
        this.numberOfPhases = numberOfPhases;
    }
    
    public PoleSummary() {}
    
    public PoleSummary(Pole p, String equipmentType, String framing, Integer numberOfPhases) {
        this.equipmentType = equipmentType;
        this.framing = framing;
        this.numberOfPhases = numberOfPhases;
        populateFrom(p);
    }
}
