package com.precisionhawk.poleams.domain.poledata;

/**
 *
 * @author pchapman
 */
public class PowerCircuit {
    
    private PrimaryCable primary;
    public PrimaryCable getPrimary() {
        return primary;
    }
    public void setPrimary(PrimaryCable primary) {
        this.primary = primary;
    }

    private PowerCircuitCable neutral;
    public PowerCircuitCable getNeutral() {
        return neutral;
    }
    public void setNeutral(PowerCircuitCable neutral) {
        this.neutral = neutral;
    }

    private SecondaryCable secondary;
    public SecondaryCable getSecondary() {
        return secondary;
    }
    public void setSecondary(SecondaryCable secondary) {
        this.secondary = secondary;
    }
}
