package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.processors.poleinspection.FeederDataDirProcessor;
import com.precisionhawk.poleams.processors.poleinspection.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ProcessStatus;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class FeederDataImportProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "importFeederData";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " path/to/feeder/data/dir";
    
    private String dirPath;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (dirPath == null) {
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
        ProcessListener listener = new ProcessListener() {
            public void setStatus(ProcessStatus processStatus) {
                System.out.printf("Status: %s\n", processStatus);
            }

            public void reportFatalError(String message) {
                System.err.println(message);
            }

            public void reportFatalException(String message, Throwable t) {
                System.err.println(message);
                t.printStackTrace(System.err);
            }

            public void reportFatalException(Exception ex) {
                ex.printStackTrace(System.err);
            }

            public void reportMessage(String message) {
                System.out.println(message);
            }

            public void reportNonFatalError(String message) {
                System.err.println(message);
            }

            public void reportNonFatalException(String message, Throwable t) {
                System.err.println(message);
                t.printStackTrace(System.err);
            }
        };
        boolean success = FeederDataDirProcessor.process(env, listener, new File(dirPath));
        System.out.printf("Import finished with %s", (success ? "success" : "errors"));
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
