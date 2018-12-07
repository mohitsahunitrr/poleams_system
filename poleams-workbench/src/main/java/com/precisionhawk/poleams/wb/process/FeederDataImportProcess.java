package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.poleams.processors.poleinspection.FeederDataDirProcessor;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessStatus;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class FeederDataImportProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_ORG_ID = "-orgId";
    private static final String COMMAND = "importFeederInspection";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_ORDER_NUM + " WorkOrderNumber " + ARG_ORG_ID + " organizationId path/to/inspection/data/dir";
    
    private String dirPath;
    private String orderNumber;
    private String orgId;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_ORDER_NUM.equals(arg)) {
            if (orderNumber == null) {
                orderNumber = args.poll();
                return orderNumber != null;
            } else {
                return false;
            }
        } else if (ARG_ORG_ID.equals(arg)) {
            if (orgId == null) {
                orgId = args.poll();
                return orgId != null;
            } else {
                return false;
            }
        } else if (dirPath == null) {
            dirPath = arg;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (dirPath == null || orderNumber == null) {
            return false;
        }
        ImportProcessListener listener = new ImportProcessListener() {
            @Override
            public void setStatus(ImportProcessStatus processStatus) {
                System.out.printf("Status: %s\n", processStatus);
            }
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
        };
        boolean success = FeederDataDirProcessor.process(env, listener, new File(dirPath), orgId, orderNumber);
        System.out.printf("Import finished with %s\n", (success ? "success" : "errors"));
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
