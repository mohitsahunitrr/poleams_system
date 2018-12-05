package com.precisionhawk.poleamsv0dot0.domain.poledata;

/**
 *
 * @author Philip A. Chapman
 */
public class SecondaryCable extends PowerCircuitCable {
    
    private Integer wireCount;
    public Integer getWireCount() {
        return wireCount;
    }
    public void setWireCount(Integer wireCount) {
        this.wireCount = wireCount;
    }

    private Boolean multiplex;
    public Boolean getMultiplex() {
        return multiplex;
    }
    public void setMultiplex(Boolean multiplex) {
        this.multiplex = multiplex;
    }
}
