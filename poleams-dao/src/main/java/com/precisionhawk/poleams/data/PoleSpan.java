package com.precisionhawk.poleams.data;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class PoleSpan {
    
    private String brearing;
    public String getBrearing() {
        return brearing;
    }
    public void setBrearing(String brearing) {
        this.brearing = brearing;
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
