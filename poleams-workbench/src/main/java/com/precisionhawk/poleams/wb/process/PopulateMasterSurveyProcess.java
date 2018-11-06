package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.processors.poleinspection.SurveyReportGenerator;
import com.precisionhawk.poleams.processors.poleinspection.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ImportProcessStatus;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
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
    private static final String ARG_UPDATE = "-u";
    private static final String COMMAND = "populateMasterSurvey";
    private static final String HELP = "\t" + COMMAND + " "
            + ARGS_FOR_HELP + " " +
            ARG_FEEDER_ID + " FeederId "
            + ARG_IN_FILE + " path/to/in/file "
            + "[ " + ARG_OUT_FILE + " path/to/out/file ] "
            + "[ " + ARG_UPDATE + "]\n"
            + "\t" + ARG_FEEDER_ID + " FeederId : The feeder to generate the master survey report for.\n"
            + "\t" + ARG_IN_FILE + " path/to/in/file : The path to the master survey report template.\n"
            + "\t" + ARG_OUT_FILE + " path/to/out/file : The path to which the survey report should be written, if desired.\n"
            + "\t" + ARG_UPDATE + " Upload the master survey report into the repository.";
    
    private String feederId;
    private String inFile;
    private String outFile;
    private boolean uploadIntoRepo = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_FEEDER_ID.equals(arg)) {
            if (feederId == null) {
                feederId = args.poll();
                return feederId != null;
            }
        } else if (ARG_IN_FILE.equals(arg)) {
            if (inFile == null) {
                inFile = args.poll();
                return inFile != null;
            }
        } else if (ARG_OUT_FILE.equals(arg)) {
            if (outFile == null) {
                outFile = args.poll();
                return outFile != null;
            }
        } else if (ARG_UPDATE.equals(arg)) {
            if (uploadIntoRepo) {
                // Already set once
                return false;
            }
            uploadIntoRepo = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        if (feederId == null) {
            return false;
        } else if (inFile == null) {
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
        try {
            File file;
            if (outFile == null) {
                file = File.createTempFile(feederId, "xlsx");
            } else {
                file = new File(outFile);
            }
            boolean success = SurveyReportGenerator.process(env, listener, feederId, new File(inFile), file);
            System.out.printf("Import finished with %s\n", (success ? "success" : "errors"));
            if (success && uploadIntoRepo) {
                ResourceUploadProcess uploadProc = new ResourceUploadProcess(feederId, null, null, ResourceType.SurveyReport, true, file.getAbsolutePath());
                return uploadProc.execute(env);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
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
