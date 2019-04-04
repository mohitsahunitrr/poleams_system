package com.precisionhawk.poleams.support.geojson;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class GeoJsonFeature {
    private GeoJsonGeometry geometry;
    public GeoJsonGeometry getGeometry() {
        return geometry;
    }
    public void setGeometry(GeoJsonGeometry geometry) {
        this.geometry = geometry;
    }
    
    private String type;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    
    private Map<String, String> properties = new HashMap();
    public Map<String, String> getProperties() {
        return properties;
    }
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
