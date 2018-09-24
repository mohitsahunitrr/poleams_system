package com.precisionhawk.poleams.domain.poledata;

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

    private Integer height;
    public Integer getHeight() {
        return height;
    }
    public void setHeight(Integer height) {
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
