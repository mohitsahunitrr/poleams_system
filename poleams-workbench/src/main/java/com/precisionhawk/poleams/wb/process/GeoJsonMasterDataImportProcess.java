package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.processors.poleinspection.GeoJsonMasterDataImport;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessStatus;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class GeoJsonMasterDataImportProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feederId";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String COMMAND = "importLineMasterData";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_FEEDER_ID + " FeederId " + ARG_ORDER_NUM + " WorkOrderNumber path/to/inspection/data/dir";
    
    private String dirPath;
    private String feederId;
    private String orderNum;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_FEEDER_ID:
                if (feederId == null) {
                    feederId = args.poll();
                    return feederId != null;
                } else {
                    return false;
                }
            case ARG_ORDER_NUM:
                if (orderNum == null) {
                    orderNum = args.poll();
                    return orderNum != null;
                } else {
                    return false;
                }
            default:
                if (dirPath == null) {
                    dirPath = arg;
                    return dirPath != null;
                } else {
                    return false;
                }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (dirPath == null || orderNum == null) {
            return false;
        }
        ImportProcessListener listener = new ImportProcessListener() {
            @Override
            public void reportFatalError(String message) {
                System.err.println(message);
            }
            @Override
            public void reportFatalException(String message, Throwable t) {
                System.err.println(message);
                t.printStackTrace(System.err);
            }
            @Override
            public void reportFatalException(Exception ex) {
                ex.printStackTrace(System.err);
            }
            @Override
            public void reportMessage(String message) {
                System.out.println(message);
            }
            @Override
            public void reportNonFatalError(String message) {
                System.err.println(message);
            }
            @Override
            public void reportNonFatalException(String message, Throwable t) {
                System.err.println(message);
                t.printStackTrace(System.err);
            }

            @Override
            public void setStatus(ImportProcessStatus processStatus) {
                reportMessage(String.format("Status: %s", processStatus));
            }
        };
        GeoJsonMasterDataImport importer = new GeoJsonMasterDataImport();
        importer.process(env, listener, feederId, orderNum, new File(dirPath));
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
    }
    
}
