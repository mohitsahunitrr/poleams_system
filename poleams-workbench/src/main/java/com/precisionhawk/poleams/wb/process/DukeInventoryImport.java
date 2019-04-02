package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.duke.ShapeFilesMasterDataImport;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class DukeInventoryImport extends ServiceClientCommandProcess {
    
    private static final String ARG_ONE_FEEDER = "-1";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String COMMAND = "dukeInventoryImporter";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_ORDER_NUM + " WorkOrderNumber [" + ARG_ONE_FEEDER + "] path/to/inventory/data/dir";

    private boolean expect1Feeder = false;
    private String orderNumber;
    private String dataPath;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_ONE_FEEDER:
                if (expect1Feeder) {
                    return false;
                } else {
                    expect1Feeder = true;
                    return true;
                }
            case ARG_ORDER_NUM:
                if (orderNumber == null) {
                    orderNumber = args.poll();
                    return orderNumber != null;
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
        } else if (dataPath == null || dataPath.isEmpty()) {
            System.err.println("Data path required.");
        }
        ProcessListener listener = new CLIProcessListener();
        ShapeFilesMasterDataImport.process(env, listener, new File(dataPath), orderNumber, expect1Feeder);
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
