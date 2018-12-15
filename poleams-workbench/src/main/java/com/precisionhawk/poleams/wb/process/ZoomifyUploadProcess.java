package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.ImageScaleRequest;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.ContentTypeUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.ResourceTypes;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pchapman
 */
public class ZoomifyUploadProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "uploadZoomify";
    
    private static final String ARG_RESOURCE_ID = "-resourceId";

    private static final String HELP =
            "\t" + COMMAND
            + " [" + ARG_RESOURCE_ID + " ResourceId] "
            + " path/to/resource"
            ;

    private String fileName;
    private String resourceId;
    
    public ZoomifyUploadProcess() {}
        
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
                if (fileName == null) {
                    fileName = arg;
                    return true;
                }
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (resourceId == null || fileName == null) {
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
            
            File zifFile = new File(fileName);
            if (zifFile.isFile() && zifFile.canRead()) {
               
                if (rmeta.getZoomifyId() == null) {
                    rmeta.setZoomifyId(UUID.randomUUID().toString());
                    rmeta = rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                }
                
                HttpClientUtilities.postFile(env, rmeta.getZoomifyId(), "image/zif", zifFile);

                System.out.printf("The resource with Id %s has been updated with zoomify ID of %s\n", resourceId, rmeta.getZoomifyId());
            } else {
                System.err.printf("The file %s does not exist, is not a file, or cannot be read.\n", fileName);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ZoomifyUploadProcess.class.getName()).log(Level.SEVERE, null, ex);
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
