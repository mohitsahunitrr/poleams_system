package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.util.HttpClientUtil;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

/**
 * Changes status of images with orientation != 1 so that they are re-processed
 * by Zoomify.
 *
 * @author pchapman
 */
public class ReZoomify extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "rezoomify";

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        ResourceSearchParams params = new ResourceSearchParams();
        WSClientHelper svcs = new WSClientHelper(env);
        TiffImageMetadata exif;
        File f;
        Short orientation;
        OutputStream os;
        ResourceMetadata rmeta;
        ResourceWebService svc = svcs.resources();
        try {
            for (Feeder feeder : svcs.feeders().retrieveAll(svcs.token())) {
                params.setSiteId(feeder.getId());
                for (ResourceSummary smry : svc.querySummaries(svcs.token(), params)) {
                    try {
                        if (smry.getZoomifyURL() != null) {
                            f = File.createTempFile("pams", "." + ImageUtilities.ImageType.fromContentType(smry.getContentType()).toExtension());
                            HttpClientUtil.downloadResource(smry.getDownloadURL(), new FileOutputStream(f));
                            exif = ImageUtilities.retrieveExif(f);
                            orientation = ImageUtilities.readImageOrientation(exif);
                            if (orientation != null && orientation != TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL) {
                                // Re-queue
                                rmeta = svc.retrieve(svcs.token(), smry.getResourceId());
                                System.out.printf("Requeuing %s for processing as it has orientation %d\f", rmeta.getResourceId(), orientation);
                                rmeta.setStatus(ResourceStatus.Processed);
                                svc.updateResourceMetadata(svcs.token(), rmeta);
                            }
                            f.delete();
                        }
                    } catch (Exception ex) {
                        System.err.printf("Unable to process resource %s\n", smry.getResourceId());
                        ex.printStackTrace(System.err);
                    }
                }
            }
        } catch (Exception ex) {
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
    }
    
}
