package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.bean.ImageScaleRequest;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.ResourceTypes;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Philip A Chapman
 */
public final class ResourceDataUploader {
    
    private static final double SCALE_WIDTH = 100;
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResourceType(ResourceTypes.ThumbNail);
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(SCALE_WIDTH);
    }
    
    public static void uploadResources(
        Environment env, ProcessListener listener, InspectionDataInterface inspdata, Collection<ResourceMetadata> metadata, Map<String, File> data, int retryCount
    )
    {
        Map<String, ResourceMetadata> map = new HashMap<>();
        for (ResourceMetadata rmeta : metadata) {
            map.put(rmeta.getResourceId(), rmeta);
        }
        _uploadResources(env, listener, inspdata, map, data, retryCount);
    }
    
    private static void _uploadResources(
        Environment env, ProcessListener listener, InspectionDataInterface inspdata, Map<String, ResourceMetadata> metadata, Map<String, File> data, int retryCount
    )
    {
        Boolean b;
        File dataFile;
        int count = 1;
        Map<String, Boolean> exists;
        List<String> ids = new ArrayList<>(metadata.keySet());
        ResourceMetadata rmeta;
        boolean success = false;
        ResourceWebService svc = env.obtainWebService(ResourceWebService.class);

        for (int i = 0; (!success) && i < retryCount; i++) {
            try {
                exists = svc.verifyUploadedResources(env.obtainAccessToken(), ids);
                success = true;
                for (String resourceId : metadata.keySet()) {
                    b = exists.get(resourceId);
                    rmeta = metadata.get(resourceId);
                    if (b == null || (!b) || ResourceStatus.QueuedForUpload == rmeta.getStatus()) {
                        success = false;
                        if (rmeta.getResourceId() == null || inspdata.getDomainObjectIsNew().get(rmeta.getResourceId())) {
                            if (rmeta.getResourceId() == null) {
                                rmeta.setResourceId(UUID.randomUUID().toString());
                            }
                            rmeta.setStatus(ResourceStatus.QueuedForUpload);
                            svc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
                            try {
                                Thread.sleep(250); // Pause .25 second to ensure ElasticSearch has had time to injest.
                            } catch (InterruptedException ex) {
                                // DO Nothing
                            }
                            inspdata.getDomainObjectIsNew().put(rmeta.getResourceId(), false);
                        } else {
                            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                        }
                        dataFile = data.get(resourceId);
                        listener.reportMessage(String.format("Uploading file \"%s\" for resource \"%s\", attempt %d (total: %d of %d)", dataFile, rmeta.getResourceId(), (i + 1), count++, exists.size()));
                        HttpClientUtilities.postFile(env, rmeta.getResourceId(), rmeta.getContentType(), dataFile);
                        rmeta.setStatus(ResourceStatus.Uploaded);
                        svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                        if (
                                ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) != null
                                && rmeta.getSize() != null
                                && rmeta.getSize().getWidth() > SCALE_WIDTH
                            )
                        {
                            // Generate a thumbnail for the image.
                            try {
                                Thread.sleep(1250); // Pause 1.25 second to ensure S3 has had time to injest.
                            } catch (InterruptedException ex) {
                                // DO Nothing
                            }
                            svc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
                        }
                        if (ResourceTypes.DroneInspectionImage.equals(rmeta.getType())) {
                            // Queue the image to be zoomified.
                            rmeta.setStatus(ResourceStatus.Processed);
                        } else {
                            // Mark the resource ready for user consumption
                            rmeta.setStatus(ResourceStatus.Released);
                        }
                        svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                    }
                }
            } catch (IOException | URISyntaxException ex) {
                listener.reportNonFatalException("Error uploading resource.", ex);
            }
        }
        
        // Report any resources that were not successfully uploaded.
        try {
            exists = svc.verifyUploadedResources(env.obtainAccessToken(), ids);
            for (String resourceId : metadata.keySet()) {
                b = exists.get(resourceId);
                rmeta = metadata.get(resourceId);
                if (b == null || (!b) || ResourceStatus.QueuedForUpload == rmeta.getStatus()) {
                    listener.reportNonFatalError(String.format("The data for Resource %s located at %s could not be uploaded after %d tries.", resourceId, data.get(resourceId), retryCount));
                }
            }
        } catch (IOException ex) {
            listener.reportNonFatalException("Error checking to see if all resources have been uploaded.", ex);
        }
    }
}
