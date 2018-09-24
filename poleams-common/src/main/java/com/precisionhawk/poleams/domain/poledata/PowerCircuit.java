package com.precisionhawk.poleams.domain.poledata;

/**
 *
 * @author Philip A. Chapman
 */
public class PowerCircuit {
    
    private PrimaryCable primary;
    public PrimaryCable getPrimary() {
        return primary;
    }
    public void setPrimary(PrimaryCable primary) {
        this.primary = primary;
    }

    private NeutralCable neutral;
    public NeutralCable getNeutral() {
        return neutral;
    }
    public void setNeutral(NeutralCable neutral) {
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
