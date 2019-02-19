package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.domain.WorkOrderType;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String COMMAND = "fixData";
    private static final File parentDir=new File("/opt/old/tmp/bar/PrecisionAnalytics/Data Analytics");
    
    // Fix inspection data for Line 10
    private boolean dry = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_DRY.equals(arg)) {
            if (dry) {
                // Only pass the arg once
                return false;
            } else {
                dry = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper services = new WSClientHelper(env);
        try {
            AssetInspectionType bad = new AssetInspectionType("Pole Inspection");
            AssetInspectionType good = new AssetInspectionType("DistributionLineInspection");
            AssetInspectionSearchParams params = new AssetInspectionSearchParams();
            params.setSiteId("908d7dae-5c67-42f0-9cc5-08137205a566");
            for (PoleInspection insp : services.poleInspections().search(services.token(), params)) {
                if (!good.equals(insp.getType())) {
                    insp.setType(good);
                    services.poleInspections().update(services.token(), insp);
                }
            }
            WorkOrder wo = services.workOrders().retrieveById(services.token(), "9C44C721");
            wo.setType(WorkOrderTypes.DistributionLineInspection);
            services.workOrders().update(services.token(), wo);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }
    
    private File findImageFile(File parent, String name) {
        if (parent.isDirectory()) {
            for (File f : parent.listFiles()) {
                parent = findImageFile(f, name);
                if (parent != null) {
                    return parent;
                }
            }
            return null;
        } else if (parent.getName().equals(name)) {
            return parent;
        } else {
            return null;
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {}
}
