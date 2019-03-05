package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.poleinspection.GeoJsonMasterDataImport;
import com.precisionhawk.poleams.processors.MasterDataImporter;
import com.precisionhawk.poleams.processors.poleinspection.ShapeFileMasterDataImport;
import java.io.File;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class DistributionMasterDataImportProcess extends ServiceClientCommandProcess {
    
    enum InputType {
//        GeoJson(ARG_FEEDER_ID),
        ShapeFile(ARG_ORG_ID);

        final String requires;
        InputType(String requires) {
            this.requires = requires;
        }
    }
    
    private static final String ARG_FEEDER_ID = "-feederId";
    //TODO: Make order number optional?  This is master data import, after all.
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_ORG_ID = "-orgId";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "importDistMasterData";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_TYPE + " inputType [" + ARG_FEEDER_ID + " FeederId] [" + ARG_ORG_ID + " organizationId] " + ARG_ORDER_NUM + " WorkOrderNumber path/to/inspection/data/dir";
    
    private String dirPath;
    private String feederId;
    private InputType inputType;
    private String orderNum;
    private String orgId;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_FEEDER_ID:
                if (feederId == null) {
                    feederId = args.poll();
                    return feederId != null;
                } else {
                    return false;
                }
            case ARG_ORDER_NUM:
                if (orderNum == null) {
                    orderNum = args.poll();
                    return orderNum != null;
                } else {
                    return false;
                }
            case ARG_ORG_ID:
                if (orgId == null) {
                    orgId = args.poll();
                    return orgId != null;
                } else {
                    return false;
                }
            case ARG_TYPE:
                if (inputType == null) {
                    String s = args.poll();
                    if (s != null) {
                        try {
                            inputType = InputType.valueOf(s);
                        } catch (IllegalArgumentException ex) {
                            inputType = null;
                        }
                    }
                    return inputType != null;
                } else {
                    return false;
                }
            default:
                if (dirPath == null) {
                    dirPath = arg;
                    return dirPath != null;
                } else {
                    return false;
                }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (dirPath == null || orderNum == null) {
            return false;
        }
        MasterDataImporter importer;
        String otherData;
        switch (inputType) {
//            case GeoJson:
//                otherData = feederId;
//                importer = new GeoJsonMasterDataImport();
//                break;
            case ShapeFile:
                otherData = orgId;
                importer = new ShapeFileMasterDataImport();
                break;
            default:
                return false;
        }
        if (inputType.requires != null && otherData == null) {
            System.err.printf("%s is required for input type %s\n", inputType.requires, inputType.name());
            return false;
        }
        importer.process(env, new CLIProcessListener(), new File(dirPath), orderNum, otherData);
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
        output.println("\t\tAvailable Input Types:");
        for (InputType type : InputType.values()) {
            output.printf("\t\t\t%s", type.name());
            if (type.requires != null) {
                output.printf("(requires %s)\n", type.requires);
            } else {
                output.printf("\n");
            }
        }
    }
    
}
