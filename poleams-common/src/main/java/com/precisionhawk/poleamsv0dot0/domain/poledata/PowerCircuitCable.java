package com.precisionhawk.poleams.domain.poledata;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class PowerCircuitCable {

    private String conductor;
    public String getConductor() {
        return conductor;
    }
    public void setConductor(String conductor) {
        this.conductor = conductor;
    }
}
