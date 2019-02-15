package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.poleams.processors.poleinspection.FeederDataDirProcessor;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.FeederDataDirProcessor2;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class FeederDataImportProcess extends ServiceClientCommandProcess {
    
    public enum Type{
        CSV,
        Excel
    }
    
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_ORG_ID = "-orgId";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "importFeederInspection";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_TYPE + "[" + Type.CSV.name() + "|" + Type.Excel.name() + "] " + ARG_ORDER_NUM + " WorkOrderNumber " + ARG_ORG_ID + " organizationId path/to/inspection/data/dir";
    
    private String dirPath;
    private String orderNumber;
    private String orgId;
    private Type type;

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
        } else if (ARG_TYPE.equals(arg)) {
            if (type == null) {
                type = Type.valueOf(args.poll());
                return true;
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
        ProcessListener listener = new ProcessListener() {
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
        boolean success = false;
        if (type == Type.CSV) {
            success = FeederDataDirProcessor2.process(env, listener, new File(dirPath), orgId, orderNumber);
        } else if (type == Type.Excel) {
            success = FeederDataDirProcessor.process(env, listener, new File(dirPath), orgId, orderNumber);        
        } else {
            return false;
        }
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
