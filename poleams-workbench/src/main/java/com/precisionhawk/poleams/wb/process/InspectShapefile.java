package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.config.WorkbenchConfig;
import com.precisionhawk.ams.wb.process.CommandProcess;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
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
public class InspectShapefile extends CommandProcess {
    
    private static final String COMMAND = "inspectShapefile";
    private static final String HELP = COMMAND + " /path/file.shp";

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
    }

    @Override
    public boolean process(WorkbenchConfig config, Queue<String> args) {
        String shapeFilePath = null;
        for (String arg = args.poll() ; arg != null ; arg = args.poll()) {
            if (shapeFilePath == null) {
                shapeFilePath = arg;
            } else {
                return false;
            }
        }
        if (shapeFilePath == null || shapeFilePath.isEmpty()) {
            System.err.println("Shape file path is required.");
            return false;
        }
        
        try {
            File file = new File(shapeFilePath);
            Map<String, Object> map = new HashMap<>();
            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source =
                    dataStore.getFeatureSource(typeName);
            Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

            Map<String, PropertyInfo> propertyInfo = new HashMap();
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
            try (FeatureIterator<SimpleFeature> features = collection.features()) {
                while (features.hasNext()) {
                    SimpleFeature feature = features.next();
                    System.out.printf("%s: %s\n", URLDecoder.decode(feature.getID(), "UTF-8"), feature.getDefaultGeometryProperty().getValue());
                    for (Property p : feature.getProperties()) {
                        System.out.printf("\t%s: %s\n", p.getName(), p.getValue());
                        PropertyInfo info = propertyInfo.get(p.getName().toString());
                        if (info == null) {
                            info = new PropertyInfo(p);
                            propertyInfo.put(info.getName(), info);
                        } else {
                            info.update(p);
                        }
                    }
                }
            }
            System.out.println("\nProperties:");
            for (PropertyInfo info : propertyInfo.values()) {
                System.out.printf("\tName: %s\tType: %s\tExample: %s\n", info.getName(), info.getType(), info.getExampleValue());
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        
        return true;
    }
    
    class PropertyInfo {
        Object exampleValue;
        String name;
        String type;

        PropertyInfo() {}
        
        PropertyInfo(Property prop) {
            name = prop.getName().toString();
//            type = prop.getType().getName().toString();
            update(prop);
        }
        
        public Object getExampleValue() {
            return exampleValue;
        }
        public void setExampleValue(Object exampleValue) {
            this.exampleValue = exampleValue;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        
        final void update(Property prop) {
            if (exampleValue == null) {
                Object o = prop.getValue();
                if (o != null) {
                    exampleValue = o.toString();
                    type = o.getClass().getSimpleName();
                }
            }
        }
    }
}
