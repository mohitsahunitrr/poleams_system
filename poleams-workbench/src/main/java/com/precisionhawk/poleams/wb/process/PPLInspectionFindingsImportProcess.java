package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.ppl.PPLConstants;
import com.precisionhawk.poleams.processors.poleinspection.ppl.PPLInspectionFindingsImport;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class PPLInspectionFindingsImportProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String COMMAND = "pplInspectionFindings";

    private String argDir;
    private boolean dryRun = false;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_DRY:
                if (dryRun) {
                    return false;
                } else {
                    dryRun = true;
                    return true;
                }
            default:
                if (argDir == null) {
                    argDir = arg;
                    return argDir != null;
                }
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (argDir == null || argDir.isEmpty()) {
            return false;
        }
        Console c = System.console();
        try {
            Feeder f = chooseFeeder(env, c, PPLConstants.ORG_ID);
            if (f == null) {
                return true;
            }
            
            WorkOrder wo = chooseWorkOrder(env, c, f);
            if (wo == null) {
                return true;
            }
            
            c.printf("\nProcessing...\n");
            
            ProcessListener l = new CLIProcessListener();
            PPLInspectionFindingsImport.process(env, l, new File(argDir), f.getId(), wo.getOrderNumber(), dryRun);
            
            c.printf("\nDone\n");
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
        output.printf("\t%s %s [%s] /path/to/data/dir\n", COMMAND, ARGS_FOR_HELP, ARG_DRY);
    }
    
}
