package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 * The load case used to analyze the pole.
 *
 * @author pchapman
 */
@Schema(description="The load case used to analyze the pole.")
public class PoleAnalysisLoadCase {
    
    @Schema(description="The maximum ice load.")
    private String ice;
    public String getIce() {
        return ice;
    }
    public void setIce(String ice) {
        this.ice = ice;
    }

    @Schema(description="NESC rule used for analysis.")
    private String nescRule;
    public String getNescRule() {
        return nescRule;
    }
    public void setNescRule(String nescRule) {
        this.nescRule = nescRule;
    }

    @Schema(description="The maximum temperature.")
    private String temperature;
    public String getTemperature() {
        return temperature;
    }
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    @Schema(description="The maximum wind speed.")
    private String wind;
    public String getWind() {
        return wind;
    }
    public void setWind(String wind) {
        this.wind = wind;
    }
}
