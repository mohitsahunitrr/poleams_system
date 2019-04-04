package com.precisionhawk.poleams.support.geojson;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class GeoJsonCRS {
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    private String type;
    private Map<String, String> properties = new HashMap();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
