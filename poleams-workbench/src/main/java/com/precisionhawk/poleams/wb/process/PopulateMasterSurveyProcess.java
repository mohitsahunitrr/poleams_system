package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.processors.poleinspection.SurveyReportGenerator;
import com.precisionhawk.poleams.processors.poleinspection.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessStatus;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class PopulateMasterSurveyProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-f";
    private static final String COMMAND = "populateMasterSurvey";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_FEEDER_ID + " FeederId  path/to/feeder/data/dir";
    
    private String feederId;
    private String filePath;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_FEEDER_ID.equals(arg)) {
            feederId = args.poll();
            if (feederId != null) {
                return true;
            }
        } else if (filePath == null) {
            filePath = arg;
            return true;
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        if (filePath == null) {
            return false;
        }
        ProcessListener listener = new ProcessListener() {
            public void setStatus(ImportProcessStatus processStatus) {
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
        boolean success = SurveyReportGenerator.process(env, listener, feederId, new File(filePath));
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
