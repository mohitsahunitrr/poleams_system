package com.precisionhawk.poleams.domain.poledata;

/**
 *
 * @author pchapman
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
}
