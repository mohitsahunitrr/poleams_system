package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author Philip A. Chapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FeederDataDirProcessor.class);
    
    private static final String POLE_DATA_SUBDIR = "Pole_Photos_and_PF_Project_V2";
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment env, ProcessListener listener, File feederDir) {
        
        listener.setStatus(ProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        // There should be an excel file for all poles.
        boolean success = MasterSurveyTemplateProcessor.processMasterSurveyTemplate(env, listener, data, feederDir);
        
        if (success) {
            File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
            if (poleDataDir.exists() && poleDataDir.canRead()) {
                listener.setStatus(ProcessStatus.ProcessingPoleData);
                for (File f : poleDataDir.listFiles()) {
                    if (f.isDirectory()) {
                        // The name of the directory should be the FPL ID of the pole.
                        PoleData pole = data.getPoleDataByFPLId().get(f.getName());
                        if (pole == null) {
                            listener.reportMessage(String.format("No pole found with FPL ID \"%s\".  The directory \"%s\" is being skipped.", f.getName(), f.getAbsolutePath()));
                        } else {
                            success = PoleDataProcessor.process(env, listener, data, pole, f);
                            if (!success) {
                                break;
                            }
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
            listener.setStatus(ProcessStatus.PersistingData);
            
            // Save SubStation
            if (data.getSubStation() != null) {
                SubStationWebService sssvc = env.obtainWebService(SubStationWebService.class);
                if (data.getDomainObjectIsNew().get(data.getSubStation().getId())) {
                    sssvc.create(env.obtainAccessToken(), data.getSubStation());
                } else {
                    sssvc.update(env.obtainAccessToken(), data.getSubStation());
                }
            }

            // Save Poles
            if (!data.getPoleDataByFPLId().isEmpty()) {
                PoleWebService psvc = env.obtainWebService(PoleWebService.class);
                for (PoleData pdata : data.getPoleDataByFPLId().values()) {
                    if (data.getDomainObjectIsNew().get(pdata.getId())) {
                        psvc.create(env.obtainAccessToken(), pdata);
                    } else {
                        psvc.update(env.obtainAccessToken(), pdata);
                    }
                }
            }
            
            // Save Pole Inspections
            if (!data.getPoleInspectionsByFPLId().isEmpty()) {
                PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
                for (PoleInspection pi : data.getPoleInspectionsByFPLId().values()) {
                    if (data.getDomainObjectIsNew().get(pi.getId())) {
                        pisvc.create(env.obtainAccessToken(), pi);
                    } else {
                        pisvc.update(env.obtainAccessToken(), pi);
                    }
                }
            }
            
            // Save Resources
            ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
            for (ResourceMetadata rmeta : data.getSubStationResources()) {
                saveResource(env, rsvc, listener, data, rmeta);
            }
            for (List<ResourceMetadata> rmetaList : data.getPoleResources().values()) {
                for (ResourceMetadata rmeta : rmetaList) {
                    saveResource(env, rsvc, listener, data, rmeta);
                }
            }
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
        
        listener.setStatus(ProcessStatus.Done);
        
        return success;
    }
    
    private static final String UPLOAD_URL = "%s/resource/%s/upload";
    private static final double SCALE_WIDTH = 100;
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(SCALE_WIDTH);
    }
    
    private static void saveResource(Environment env, ResourceWebService svc, ProcessListener listener, InspectionData data, ResourceMetadata rmeta)
        throws IOException, URISyntaxException
    {
        if (rmeta == null) {
            return;
        }
        File dataFile = data.getResourceDataFiles().get(rmeta.getResourceId());
        if (dataFile == null) {
            listener.reportNonFatalError(String.format("Missing data file for image %s for pole %s", rmeta.getName(), rmeta.getPoleId()));
            return;
        }
        if (rmeta.getResourceId() == null || data.getDomainObjectIsNew().get(rmeta.getResourceId())) {
            if (rmeta.getResourceId() == null) {
                rmeta.setResourceId(UUID.randomUUID().toString());
            }
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            svc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
        } else {
            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
        } if (ResourceStatus.QueuedForUpload == rmeta.getStatus()) {
            String url = String.format(UPLOAD_URL, env.getServiceURI(), rmeta.getResourceId());
            HttpClientUtilities.postFile(new URI(url), env.obtainAccessToken(), rmeta.getContentType(), dataFile);
            rmeta.setStatus(ResourceStatus.Uploaded);
            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
            if (
                    ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) != null
                    && rmeta.getSize() != null
                    && rmeta.getSize().getWidth() > SCALE_WIDTH
                )
            {
                // Generate a thumbnail for the image.
                svc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
            }
            if (ResourceType.DroneInspectionImage == rmeta.getType()) {
                // Queue the image to be zoomified.
                rmeta.setStatus(ResourceStatus.Processed);
            } else {
                // Mark the resource ready for user consumption
                rmeta.setStatus(ResourceStatus.Released);
            }
            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
        }
    }
}
