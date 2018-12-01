package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.processors.poleinspection.ImportProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessStatus;
import com.precisionhawk.poleams.processors.poleinspection.simple.SimplePoleInspectionImport;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class SimpleInspectionDataImportProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "simpleInspectionImport";
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " path/to/feeder/data/dir";
    
    private String feederId;
    private String dirPath;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_FEEDER_ID.equals(arg)) {
            feederId = args.poll();
            return feederId != null;
        } else if (dirPath == null) {
            dirPath = arg;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (dirPath == null) {
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
        boolean success = SimplePoleInspectionImport.process(env, listener, feederId, new File(dirPath));
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
