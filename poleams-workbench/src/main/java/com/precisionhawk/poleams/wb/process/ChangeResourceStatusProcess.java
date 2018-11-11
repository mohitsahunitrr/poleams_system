package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class ChangeResourceStatusProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_RESOURCE_ID = "-resourceId";
    private static final String ARG_STATUS = "-status";
    private static final String COMMAND = "changeResourceStatus";

    private static final String HELP =
            "\t" + COMMAND + " " + ARG_RESOURCE_ID + " resourceId "+ ARG_STATUS;

    private String resourceId;
    private ResourceStatus resourceStatus;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_RESOURCE_ID:
                if (resourceId == null) {
                    resourceId = args.poll();
                    return resourceId != null;
                } else {
                    return false;
                }
            case ARG_STATUS:
                if (resourceStatus == null) {
                    resourceStatus = ResourceStatus.valueOf(args.poll());
                    return resourceStatus != null;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (resourceId == null) {
            return false;
        } else if (resourceStatus == null) {
            return false;
        } else {
            try {
                ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
                ResourceMetadata rmeta = rsvc.retrieve(env.obtainAccessToken(), resourceId);
                
                if (rmeta == null) {
                    System.err.printf("No resource with ID \"%s\" found.", resourceId);
                } else {
                    rmeta.setStatus(resourceStatus);
                    rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
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
