package com.precisionhawk.poleamsv0dot0.domain.poledata;

/**
 *
 * @author Philip A. Chapman
 */
public class CommunicationsCable {
    
    public enum Type {CaTV, Telco}
    
    private Float diameter;
    public Float getDiameter() {
        return diameter;
    }
    public void setDiameter(Float diameter) {
        this.diameter = diameter;
    }

    private Float height;
    public Float getHeight() {
        return height;
    }
    public void setHeight(Float height) {
        this.height = height;
    }

    private Type type;
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }

    public CommunicationsCable() {}

    public CommunicationsCable(Type type) {
        this.type = type;
    }
}
