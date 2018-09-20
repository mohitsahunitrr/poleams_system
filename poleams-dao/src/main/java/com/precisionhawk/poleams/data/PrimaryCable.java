package com.precisionhawk.poleams.data;

/**
 *
 * @author pchapman
 */
public class PrimaryCable extends PowerCircuitCable {
    
    private Integer phases;
    public Integer getPhases() {
        return phases;
    }
    public void setPhases(Integer phases) {
        this.phases = phases;
    }

    private String framing;
    public String getFraming() {
        return framing;
    }
    public void setFraming(String framing) {
        this.framing = framing;
    }
}
