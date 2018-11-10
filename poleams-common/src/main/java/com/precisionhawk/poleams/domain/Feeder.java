package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.Site;
import io.swagger.oas.annotations.media.Schema;

/**
 * A power substation.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="Data related to power substation to which poles are related.")
public class Feeder extends Site {
    
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
    
    @Schema(description="The wind zone that the substation is located in, e.g. 145 = rated for up to 145 MPH winds.")
    private String windZone;
    public String getWindZone() {
        return windZone;
    }
    public void setWindZone(String windZone) {
        this.windZone = windZone;
    }
}
