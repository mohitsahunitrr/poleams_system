package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.poleinspection.fpl.FeederDataDirProcessor;
import com.precisionhawk.poleams.processors.poleinspection.duke.FeedersFromCSVProcessor;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.fpl.FeederDataDirProcessor2;
import com.precisionhawk.poleams.processors.poleinspection.duke.GeoJsonMasterDataImport;
import com.precisionhawk.poleams.processors.poleinspection.ppl.PPLInspectionDataImport;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author Philip A. Chapman
 */
public class FeederDataImportProcess extends ServiceClientCommandProcess {
    
    public enum Type{
        FPLCSV,
        FPLExcel,
        GeoJson, // Developed for Duke import
        PPL
    }
    
    private static final String ARG_DRY_RUN = "-dry";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_ORG_ID = "-orgId";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "importFeederInspection";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_TYPE + "[" + Type.FPLCSV.name() + "|" + Type.FPLExcel.name() + "|" + Type.PPL + "] " + ARG_ORDER_NUM + " WorkOrderNumber " + ARG_ORG_ID + " organizationId path/to/inspection/data/dir";
    
    private String dirPath;
    private boolean dryRun = false;
    private String orderNumber;
    private String orgId;
    private Type type;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_DRY_RUN.equals(arg)) {
            if (dryRun) {
                return false;
            }
            dryRun = true;
            return true;
        } else if (ARG_ORDER_NUM.equals(arg)) {
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
        ProcessListener listener = new CLIProcessListener();
        boolean success = false;
        if (type == Type.FPLCSV) {
            success = FeederDataDirProcessor2.process(env, listener, new File(dirPath), orgId, orderNumber, dryRun);
        } else if (type == Type.GeoJson) {
            //FIXME: This is hardcoded Developed for Duke import
            InspectionData data = FeedersFromCSVProcessor.process(env, listener, new File("/home/pchapman/tmp/duke/circuits.csv"), orgId);
            GeoJsonMasterDataImport importer = new GeoJsonMasterDataImport();
            success = importer.process(env, listener, data, new File(dirPath), orderNumber);
        } else if (type == Type.FPLExcel) {
            success = FeederDataDirProcessor.process(env, listener, new File(dirPath), orgId, orderNumber);        
        } else if (type == Type.PPL) {
            PPLInspectionDataImport importer = new PPLInspectionDataImport();
            success = importer.process(env, listener, orderNumber, new File(dirPath), dryRun);
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
