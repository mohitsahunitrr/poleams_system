package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.poleams.processors.ShapeFileProcessor;

/**
 *
 * @author pchapman
 */
public interface ShapeFileConstants {
    
    static final String PROP_NETWORK_ID = "NETWORK_ID";
    static final String[] PROP_POLE_ID = {"POLEID", "POLE_ID"};
    static final String PROP_POLE_NUM = "POLE_NUMBE";

    static final String[] PROPS_TO_REMOVE = {
        ShapeFileProcessor.PROP_LATITUDE,
        ShapeFileProcessor.PROP_LONGITUDE,
        "NETWORK_ID",
        "POLEID",
        "POLE_ID",
        "X	Type",
        "Y	Type",
        "the_geom"
    };
}
