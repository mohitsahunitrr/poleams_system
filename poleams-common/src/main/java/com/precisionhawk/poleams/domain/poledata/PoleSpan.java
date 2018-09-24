package com.precisionhawk.poleams.domain.poledata;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Philip A. Chapman
 */
public class PoleSpan {
    
    private String bearing;
    public String getBearing() {
        return bearing;
    }
    public void setBearing(String bearing) {
        this.bearing = bearing;
    }

    private String length;
    public String getLength() {
        return length;
    }
    public void setLength(String length) {
        this.length = length;
    }

    private PowerCircuit powerCircuit;
    public PowerCircuit getPowerCircuit() {
        return powerCircuit;
    }
    public void setPowerCircuit(PowerCircuit powerCircuit) {
        this.powerCircuit = powerCircuit;
    }

    private List<CommunicationsCable> communications = new LinkedList<>();
    public List<CommunicationsCable> getCommunications() {
        return communications;
    }
    public void setCommunications(List<CommunicationsCable> communications) {
        this.communications = communications;
    }
}
