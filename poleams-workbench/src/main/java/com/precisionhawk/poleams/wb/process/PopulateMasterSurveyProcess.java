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
    private static final String ARG_IN_FILE = "-i";
    private static final String ARG_OUT_FILE = "-o";
    private static final String COMMAND = "populateMasterSurvey";
    private static final String HELP = "\t" + COMMAND + " "
            + ARGS_FOR_HELP + " " +
            ARG_FEEDER_ID + " FeederId "
            + ARG_IN_FILE + " path/to/in/file"
            + ARG_OUT_FILE + " path/to/out/file";
    
    private String feederId;
    private String inFile;
    private String outFile;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_FEEDER_ID.equals(arg)) {
            feederId = args.poll();
            if (feederId != null) {
                return true;
            }
        } else if (ARG_IN_FILE.equals(arg)) {
            if (inFile == null) {
                inFile = args.poll();
                return inFile != null;
            } else {
                return false;
            }
        } else if (ARG_OUT_FILE.equals(arg)) {
            if (outFile == null) {
                outFile = args.poll();
                return outFile != null;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        if (feederId == null) {
            return false;
        } else if (inFile == null) {
            return false;
        } else if (outFile == null) {
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
        boolean success = SurveyReportGenerator.process(env, listener, feederId, new File(inFile), new File(outFile));
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
