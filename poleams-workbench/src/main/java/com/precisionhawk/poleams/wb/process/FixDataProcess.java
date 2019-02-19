package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.processors.ResourceDataUploader;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String COMMAND = "fixData";
    private static final File parentDir=new File("/opt/old/tmp/bar/PrecisionAnalytics/Data Analytics");
    
    // Fix inspection data for Line 10
    private boolean dry = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_DRY.equals(arg)) {
            if (dry) {
                // Only pass the arg once
                return false;
            } else {
                dry = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper services = new WSClientHelper(env);
        try {
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setSiteId("6722c058-71d9-483f-af76-ac76a8d33ffc");
            siparams.setOrderNumber("5305478F");
            TransmissionLineInspection sinsp = CollectionsUtilities.firstItemIn(services.transmissionLineInspections().search(services.token(), siparams));
            if (sinsp == null) {
                System.err.printf("Transmission line inspection not found.");
            }
            Map<String, TransmissionStructureInspection> structureInspections = new HashMap();
            ResourceSearchParams rparams = new ResourceSearchParams();
            rparams.setSiteId("6722c058-71d9-483f-af76-ac76a8d33ffc");
            rparams.setOrderNumber("5305478F");
            for (ResourceMetadata rmeta : services.resources().search(services.token(), rparams)) {
                if (rmeta.getSourceResourceId() == null) {
                    rparams.setSourceResourceId(rmeta.getResourceId());
                    rparams.setType(ResourceTypes.ThumbNail);
                    ResourceMetadata rm2 = CollectionsUtilities.firstItemIn(services.resources().search(services.token(), rparams));
                    if (rm2 == null) {
                        // No thumbnail.  Create it.
                        rm2 = services.resources().scale(env.obtainAccessToken(), rmeta.getResourceId(), ResourceDataUploader.SCALE_IMAGE_REQ);
                        rm2.setSourceResourceId(rmeta.getResourceId());
                        rm2.setType(ResourceTypes.ThumbNail);
                        services.resources().updateResourceMetadata(services.token(), rm2);
                    } else {
                        // else, we have a thumbnail image... strange.
                        System.err.printf("Thumbnail %s for %s already exists\n", rm2.getResourceId(), rmeta.getResourceId());
                    }
                } else {
                    // Ensure source exists.
                    ResourceMetadata source = null;
                    try {
                        source = services.resources().retrieve(services.token(), rmeta.getSourceResourceId());
                    } catch (ClientResponseFailure f) {
                        if (f.getResponse().getStatus() != HttpStatus.SC_NOT_FOUND) {
                            throw new IOException(f);
                        }
                    }
                    if (source == null) {
                        importSourceFor(services, rmeta);
                    } else {
                        // else, we have a source image... strange.
                        System.err.printf("Source %s already exists\n", rmeta.getSourceResourceId());
                    }
                }
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }
    
    private File findImageFile(File parent, String name) {
        if (parent.isDirectory()) {
            for (File f : parent.listFiles()) {
                parent = findImageFile(f, name);
                if (parent != null) {
                    return parent;
                }
            }
            return null;
        } else if (parent.getName().equals(name)) {
            return parent;
        } else {
            return null;
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {}

    private void importSourceFor(WSClientHelper services, ResourceMetadata rmeta) throws IOException, URISyntaxException {
        File f = findImageFile(parentDir, rmeta.getName());
        if (f == null) {
            System.err.printf("Unable to find file %s for thumbnail image %s\n", rmeta.getName(), rmeta.getResourceId());
        } else {
            ResourceMetadata source = new ResourceMetadata();
            source.setAssetId(rmeta.getAssetId());
            source.setAssetInspectionId(rmeta.getAssetInspectionId());
            source.setComponentId(rmeta.getComponentId());
            source.setComponentInspectionId(rmeta.getComponentInspectionId());
            source.setContentType(rmeta.getContentType());
            source.setLocation(rmeta.getLocation());
            source.setName(rmeta.getName());
            source.setOrderNumber(rmeta.getOrderNumber());
            source.setPosition(rmeta.getPosition());
            source.setResourceId(rmeta.getSourceResourceId());
            source.setSiteId(source.getSiteId());
            source.setSiteInspectionId(rmeta.getSiteInspectionId());
            source.setStatus(ResourceStatus.Processed);
            source.setTimestamp(rmeta.getTimestamp());
            source.setType(ResourceTypes.DroneInspectionImage);
            services.resources().insertResourceMetadata(services.token(), source);
            HttpClientUtilities.postFile(services.getEnv(), source.getResourceId(), source.getContentType(), f);
        }
    }
}
