package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.processors.poleinspection.fpl.SurveyReportGenerator;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class PopulateMasterSurveyProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_ALL = "-a";
    private static final String ARG_FEEDER_ID = "-f";
    private static final String ARG_IN_FILE = "-i";
    private static final String ARG_ORDER_NUM = "-n";
    private static final String ARG_OUT_FILE = "-o";
    private static final String ARG_UPDATE = "-u";
    private static final String COMMAND = "populateMasterSurvey";
    private static final String HELP = "\t" + COMMAND + " "
            + ARGS_FOR_HELP + " " +
            ARG_FEEDER_ID + " FeederId "
            + ARG_IN_FILE + " path/to/in/file "
            + "[ " + ARG_OUT_FILE + " path/to/out/file ] "
            + "[ " + ARG_UPDATE + "]\n"
            + "\t\t" + ARG_FEEDER_ID + " FeederId : The feeder to generate the master survey report for.\n"
            + "\t\t" + ARG_IN_FILE + " path/to/in/file : The path to the master survey report template.\n"
            + "\t\t" + ARG_OUT_FILE + " path/to/out/file : The path to which the survey report should be written, if desired.\n"
            + "\t\t" + ARG_ORDER_NUM + " WorkOrderNumber : The work order number.\n"
            + "\t\t" + ARG_UPDATE + " Upload the master survey report into the repository."
            + "\t\t" + ARG_ALL + " Update all fields in the template.";
    
    private String feederId;
    private String inFile;
    private String orderNumber;
    private String outFile;
    private boolean populateAll = false;
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
        } else if (ARG_ORDER_NUM.equals(arg)) {
            if (orderNumber == null) {
                orderNumber = args.poll();
                return orderNumber != null;
            } else {
                return false;
            }
        } else if (ARG_OUT_FILE.equals(arg)) {
            if (outFile == null) {
                outFile = args.poll();
                return outFile != null;
            }
        } else if (ARG_ALL.equals(arg)) {
            if (populateAll) {
                // Already set once
                return false;
            }
            populateAll = true;
            return true;
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
        if (feederId == null || inFile == null || orderNumber == null) {
            return false;
        }
        ProcessListener listener = new CLIProcessListener();
        try {
            File file;
            if (outFile == null) {
                file = File.createTempFile(feederId, "xlsx");
            } else {
                file = new File(outFile);
            }
            System.out.printf("Updating survey report for feeder %s\n", feederId);
            boolean success = SurveyReportGenerator.process(env, listener, feederId, orderNumber, new File(inFile), file, populateAll);
            System.out.printf("Import finished with %s\n", (success ? "success" : "errors"));
            if (success && uploadIntoRepo) {
                ResourceUploadProcess uploadProc = new ResourceUploadProcess(feederId, orderNumber, null, null, ResourceTypes.SurveyReport, true, file.getAbsolutePath(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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
