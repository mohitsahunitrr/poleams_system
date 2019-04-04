package com.precisionhawk.poleams.processors.poleinspection.duke;

/**
 *
 * @author pchapman
 */
public class GeoJsonGeometry {
    private Double[] coordinates;
    public Double[] getCoordinates() {
        return coordinates;
    }
    public void setCoordinates(Double[] coordinates) {
        this.coordinates = coordinates;
    }
    
    private String type;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
