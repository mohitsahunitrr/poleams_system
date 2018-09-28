package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Philip A Chapman
 */
public final class ResourceDataUploader {
    
    private static final double SCALE_WIDTH = 100;
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(SCALE_WIDTH);
    }
    
    public static void uploadResources(
        Environment env, ProcessListener listener, Collection<ResourceMetadata> metadata, Map<String, File> data, int retryCount
    )
    {
        Map<String, ResourceMetadata> map = new HashMap<>();
        for (ResourceMetadata rmeta : metadata) {
            map.put(rmeta.getResourceId(), rmeta);
        }
        _uploadResources(env, listener, map, data, retryCount);
    }
    
    private static void _uploadResources(
        Environment env, ProcessListener listener, Map<String, ResourceMetadata> metadata, Map<String, File> data, int retryCount
    )
    {
        Boolean b;
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
                        break;
                    }
                }
                if (!success) {
                    // Attempt uploads
                    for (String resourceId : metadata.keySet()) {
                        rmeta = metadata.get(resourceId);
                        HttpClientUtilities.postFile(env, rmeta.getResourceId(), rmeta.getContentType(), data.get(resourceId));
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
            } catch (IOException | URISyntaxException ex) {
                //TODO:
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
