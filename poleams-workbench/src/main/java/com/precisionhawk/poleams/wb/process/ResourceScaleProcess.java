package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.ImageScaleRequest;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.ResourceTypes;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class ResourceScaleProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "scaleImage";
    private static final double SCALE_WIDTH = 100;
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(SCALE_WIDTH);
    }
    
    private static final String ARG_RESOURCE_ID = "-resourceId";

    private static final String HELP =
            "\t" + COMMAND
            + " [" + ARG_RESOURCE_ID + " ResourceId] "
            ;

    private String resourceId;
    
    public ResourceScaleProcess() {}
        
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
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (resourceId == null) {
            return false;
        }
        try {
            ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);

            ResourceMetadata rmeta = rsvc.retrieve(env.obtainAccessToken(), resourceId);
            if (rmeta == null) {
                System.err.printf("No resource with ID \"%s\" found in data store.\n", resourceId);
                return true;
            }

            if (ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) == null) {
                System.err.printf("No resource with ID \"%s\" is not an image.\n", resourceId);
                return true;
            }
            
            if (
                    rmeta.getSize() == null
                    || rmeta.getSize().getWidth() <= SCALE_WIDTH
                )
            {
                System.err.printf("No image resource with ID \"%s\" either has no recorded size or has a width less than %f.\n", resourceId, SCALE_WIDTH);
                return true;
            } else {
                rmeta = rsvc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
            }

            System.out.printf("The resource with Id %s has been scaled with resource ID of %s\n", resourceId, rmeta.getResourceId());
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
        output.println("\t\tResource Types:");
        for (ResourceType type : ResourceTypes.values()) {
            output.printf("\t\t\t%s\n", type.getValue());
        }
    }
    
}
