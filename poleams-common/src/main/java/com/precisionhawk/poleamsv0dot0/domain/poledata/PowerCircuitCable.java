package com.precisionhawk.poleamsv0dot0.domain.poledata;

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
