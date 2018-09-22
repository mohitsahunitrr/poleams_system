package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author pchapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FeederDataDirProcessor.class);
    
    private static final String POLE_DATA_SUBDIR = "Pole_Photos_and_PF_Project_V2";
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment environment, ProcessListener listener, File feederDir) {
        
        listener.setStatus(ProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        // There should be an excel file for all poles.
        boolean success = MasterSurveyTemplateProcessor.processMasterSurveyTemplate(environment, listener, data, feederDir);
        
        if (success) {
            File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
            if (poleDataDir.exists() && poleDataDir.canRead()) {
                for (File f : poleDataDir.listFiles()) {
                    if (f.isDirectory()) {
                        success = PoleDataProcessor.process(environment, listener, data, f);
                        if (!success) {
                            break;
                        }
                    } else {
                        listener.reportMessage(String.format("File \"%s\" is not a directory and is being skipped.", f));
                    }
                }
            } else {
                listener.reportFatalError(String.format("The parent directory for pole data \"%s\" does not exist or cannot be read.", poleDataDir));
                success = false;
            }            
        }
        
        try {
            // Save SubStation
            if (data.getSubStation() != null) {
                SubStationWebService sssvc = environment.obtainWebService(SubStationWebService.class);
                if (data.getSubStation().getId() == null) {
                    sssvc.create(environment.obtainAccessToken(), data.getSubStation());
                } else {
                    sssvc.update(environment.obtainAccessToken(), data.getSubStation());
                }
            }

            // Save Poles
            if (!data.getPoleData().isEmpty()) {
                PoleWebService psvc = environment.obtainWebService(PoleWebService.class);
                for (PoleData pdata : data.getPoleData().values()) {
                    if (data.getPoleDataIsNew().get(pdata.getId())) {
                        psvc.create(environment.obtainAccessToken(), pdata);
                    } else {
                        psvc.update(environment.obtainAccessToken(), pdata);
                    }
                }
            }
            
            // Save Resources
            ResourceWebService rsvc = environment.obtainWebService(ResourceWebService.class);
            for (ResourceMetadata rmeta : data.getSubStationResources()) {
                if (rmeta.getResourceId() == null) {
                    rmeta.setResourceId(UUID.randomUUID().toString());
                    rsvc.insertResourceMetadata(environment.obtainAccessToken(), rmeta);
                } else {
                    rsvc.updateResourceMetadata(environment.obtainAccessToken(), rmeta);
                }
            }
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
        
        //TODO:
        return success;
    }
    
    private static final String UPLOAD_URL = "%s/resource/%s/upload";
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(100.0);
    }
    
    private static void saveResource(Environment env, ResourceWebService svc, ResourceMetadata rmeta, File data, boolean generateThumbnail)
        throws IOException, URISyntaxException
    {
        if (rmeta.getResourceId() == null) {
            rmeta.setResourceId(UUID.randomUUID().toString());
            svc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
        } else {
            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
        }
        String url = String.format(UPLOAD_URL, env.getServiceURI(), rmeta.getResourceId());
        HttpClientUtilities.postFile(new URI(url), ORG_ID, data);
        if (generateThumbnail) {
            svc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
        }
    }
}
