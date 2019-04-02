package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.domain.ComponentType;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ShapeFileProcessor;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 *
 * @author pchapman
 */
public class ShapeFilesMasterDataImport {
    
    private static final ComponentType[] COMP_TYPES = {
        new ComponentType("Capacitor"),
        new ComponentType("Fuse"),
        new ComponentType("Transformer"),
        new ComponentType("Primary meter"),
        new ComponentType("Recloser"),
        new ComponentType("Switch")
    };
    private static final String[] COMP_TYPE_MAPPINGS = {
        "capacitor",
        "fuse",
        "oh_xfmr",
        "primary_meter",
        "recloser",
        "switch"
    };
    
    public static final FilenameFilter COMP_SHP_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith(".shp") &&
                    (
                        name.startsWith("capacitor")
                        || name.startsWith("fuse")
                        || name.startsWith("oh_xfmr")
                        || name.startsWith("primary_meter")
                        || name.startsWith("recloser")
                        || name.startsWith("switch")
                    );
        }
    };

    private static final String DUKE_ORG_ID = "c382f193-b687-432b-b838-3049b809c937";
    
    public static final FilenameFilter POLE_SHP_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.startsWith("pole") && name.endsWith(".shp");
        }
    };
    
    private ShapeFilesMasterDataImport() {}
    
    public static boolean process(Environment env, ProcessListener listener, File dataDir, String orderNum, boolean expectOneFeeder) {
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNum);
        data.setOrganizationId(DUKE_ORG_ID);
        ShapeFileProcessor shapeFileProcessor;
        try {
            data.setCurrentWorkOrder(svcs.workOrders().retrieveById(svcs.token(), orderNum));
        } catch (Exception ex) {
            listener.reportFatalException(ex);
            return false;
        }
        if (data.getCurrentWorkOrder() == null) {
            listener.reportFatalError(String.format("Unable to look up work order %s", orderNum));
            return false;
        }
        data.addWorkOrder(data.getCurrentWorkOrder(), false);
        for (File shpFile : dataDir.listFiles(POLE_SHP_FILTER)) {
            shapeFileProcessor = new PoleShapeFileProcessor(svcs, listener, data, shpFile, expectOneFeeder);
            shapeFileProcessor.processShapeFile();
        }
        ComponentType type;
        for (File shpFile : dataDir.listFiles(COMP_SHP_FILTER)) {
            type = typeOf(shpFile);
            if (type == null) {
                listener.reportNonFatalError(String.format("Unknown component type for shape file %s", shpFile));
            } else {
                shapeFileProcessor = new ComponentShapeFileProcessor(svcs, listener, data, shpFile, type, expectOneFeeder);
                shapeFileProcessor.processShapeFile();
            }
        }
        try {
            return DataImportUtilities.saveData(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException("Error saving data", ex);
            return false;
        }
    }
    
    private static ComponentType typeOf(File file) {
        String name = file.getName().toLowerCase();
        String s;
        for (int i = 0; i < COMP_TYPE_MAPPINGS.length; i++) {
            s = COMP_TYPE_MAPPINGS[i];
            if (name.startsWith(s)) {
                return COMP_TYPES[i];
            }
        }
        return null;
    }
}
