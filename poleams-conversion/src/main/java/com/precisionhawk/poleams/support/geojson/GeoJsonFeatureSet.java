package com.precisionhawk.poleams.support.geojson;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class GeoJsonFeatureSet {
    private String name;
    private List<GeoJsonFeature> features = new LinkedList();
    private String type;
    private GeoJsonCRS crs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GeoJsonFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<GeoJsonFeature> features) {
        this.features = features;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GeoJsonCRS getCrs() {
        return crs;
    }

    public void setCrs(GeoJsonCRS crs) {
        this.crs = crs;
    }
    
}
