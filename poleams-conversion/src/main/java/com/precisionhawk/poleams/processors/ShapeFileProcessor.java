package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 *
 * @author pchapman
 */
public abstract class ShapeFileProcessor {
    
    private static final DecimalFormat LONG_INT = new DecimalFormat("############0");
    public static final String PROP_LATITUDE = "Lat";
    public static final String PROP_LONGITUDE = "Long";

    protected final InspectionData data;
    protected final ProcessListener listener;
    private final File shapeFile;
    protected final WSClientHelper svcs;

    public ShapeFileProcessor(WSClientHelper svcs, ProcessListener listener, InspectionData data, File shapeFile) {
        this.data = data;
        this.listener = listener;
        this.shapeFile = shapeFile;
        this.svcs = svcs;
    }
    
    public void processShapeFile() {        
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("url", shapeFile.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source =
                    dataStore.getFeatureSource(typeName);
            Filter filter = Filter.INCLUDE; 

            Map<String, Object> featureProps = new HashMap();
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    featureProps.clear();
                    for (Property prop : feature.getProperties()) {
                        featureProps.put(prop.getName().toString(), prop.getValue());
                    }
                    processFeature(featureProps);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    protected Object firstOf(Map<String, Object> featureProps, String[] keys) {
        Object o = null;
        for (String key : keys) {
            o = featureProps.get(key);
            if (o != null) {
                break;
            }
        }
        return o;
    }
    
    protected abstract void processFeature(Map<String, Object> featureProps);
    
    protected GeoPoint location(Map<String, Object> featureProps) {
        Double latitude = (Double)featureProps.remove(PROP_LATITUDE);
        Double longitude = (Double)featureProps.remove(PROP_LONGITUDE);
        if (latitude != null || longitude != null) {
            GeoPoint p = new GeoPoint();
            p.setLatitude(latitude);
            p.setLongitude(longitude);
            return p;
        }
        return null;
    }
    
    protected String longIntegerAsString(Object o) {
        if (o == null) {
            return null;
        }
        Double d;
        if (o instanceof Number) {
            d = ((Number)o).doubleValue();
        } else {
            d = Double.valueOf(o.toString());
        }
        return LONG_INT.format(d);
    }
    
    protected void removeKeys(Map<String, Object> featureProps, Collection<String> keys) {
        for (String key : keys) {
            featureProps.remove(key);
        }
    }
}
