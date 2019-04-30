package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.duke.DeltaShapeFileProcessor;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class DukeDeltaImport extends ServiceClientCommandProcess {
    
    private static final String ARG_ORDER_NUM = "-o";
    private static final String ARG_SITE = "-s";
    private static final String COMMAND = "dukeDeltaImporter";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_ORDER_NUM + " WorkOrderNumber " + ARG_SITE + " SiteId path/to/inspection/data.shp";

    private String dataPath;
    private String orderNumber;
    private String siteId;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_ORDER_NUM:
                if (orderNumber == null) {
                    orderNumber = args.poll();
                    return orderNumber != null;
                } else {
                    return false;
                }
            case ARG_SITE:
                if (siteId == null) {
                    siteId = args.poll();
                    return siteId != null;
                } else {
                    return false;
                }
            default:
                if (dataPath == null) {
                    dataPath = arg;
                    return !dataPath.isEmpty();
                } else {
                    return false;
                }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (orderNumber == null || orderNumber.isEmpty()) {
            System.err.println("Order number required.");
            return false;
        } else if (dataPath == null || dataPath.isEmpty()) {
            System.err.println("Data path required.");
            return false;
        } else if (siteId == null || siteId.isEmpty()) {
            System.err.println("Site ID required");
            return false;
        }
        ProcessListener listener = new CLIProcessListener();
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNumber);
        WSClientHelper svcs = new WSClientHelper(env);
        DeltaShapeFileProcessor p = new DeltaShapeFileProcessor(svcs, listener, data, new File(dataPath));
//        GeoJsonDeltaImport i = new GeoJsonDeltaImport();
//        i.process(env, listener, new File(dataPath), siteId, orderNumber);
        p.processShapeFile();
        try {
            DataImportUtilities.saveData(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        }
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.append(HELP);
    }
    
}
