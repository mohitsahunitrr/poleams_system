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
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author Philip A Chapman
 */
public final class ResourceDataUploader {
    
    private static final double SCALE_WIDTH = 100;
    public static final ImageScaleRequest SCALE_IMAGE_REQ;
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
        Map<String, Boolean> exists;
        ResourceMetadata rmeta;
        ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
        List<String> ids = new ArrayList<>(metadata.keySet());
        try {
            exists = svc.verifyUploadedResources(env.obtainAccessToken(), ids);
            for (String resourceId : metadata.keySet()) {
                rmeta = metadata.get(resourceId);
                if (data.containsKey(resourceId)) {
                    b = exists.get(resourceId);
                    if (b == null || !b || rmeta.getStatus() == ResourceStatus.QueuedForUpload) {
                        _uploadResource(env, svc, listener, inspdata, rmeta, data.get(resourceId), retryCount);
                        if (rmeta.getZoomifyId() != null && data.containsKey(rmeta.getZoomifyId())) {
                            _uploadZoomify(env, svc, listener, rmeta.getZoomifyId(), data.get(rmeta.getZoomifyId()), retryCount);
                        }
                    } else {
                        // Just save the data
                        try {
                            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                        } catch (IOException ex) {
                            listener.reportNonFatalException(String.format("Error updating resource %s.", rmeta.getResourceId()), ex);
                        }
                    }
                } else {
                    // No data file to upload.  Only metadata insert/update was desired, most likely.  Output a message just in case that was not the intent.
                    b = exists.get(resourceId);
                    if (b == null || !b) {
                        svc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
                        listener.reportMessage(String.format("No data file for resource %s.  Metadata inserted, but no data uploaded.", resourceId));
                   } else {
                        svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                        listener.reportMessage(String.format("No data file for resource %s.  Metadata updated, but no data uploaded.", resourceId));
                    }
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException("Error uploading resources", ex);
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

//    public static void uploadResource(
//        Environment env, ProcessListener listener, InspectionDataInterface inspdata, ResourceMetadata metadata, File data, int retryCount
//    )
//    {
//        ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
//        List<String> ids = new LinkedList<>();
//        ids.add(metadata.getResourceId());
//        try {
//            Map<String, Boolean> exists = svc.verifyUploadedResources(env.obtainAccessToken(), ids);
//            Boolean b = exists.get(metadata.getResourceId());
//            if (b == null || (!b) || ResourceStatus.QueuedForUpload == metadata.getStatus()) {
//                listener.reportNonFatalError(String.format("The data for Resource %s located at %s could not be uploaded after %d tries.", metadata.getResourceId(), data, retryCount));
//            } else {
//                // Just save the data
//                try {
//                    svc.updateResourceMetadata(env.obtainAccessToken(), metadata);
//                } catch (IOException ex) {
//                    listener.reportNonFatalException(String.format("Error updating resource %s.", metadata.getResourceId()), ex);
//                }
//            }
//        } catch (IOException ex) {
//            listener.reportNonFatalException("Error checking to see if all resources have been uploaded.", ex);
//        }
//    }

    private static void _uploadResource(
        Environment env, ResourceWebService svc, ProcessListener listener, InspectionDataInterface inspdata, ResourceMetadata rmeta, File dataFile, int retryCount
    )
    {
        Boolean b;
        boolean success = false;
        for (int i = 0; (!success) && i < retryCount; i++) {
            try {
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
                listener.reportMessage(String.format("Uploading file \"%s\" for resource \"%s\", attempt %d", dataFile, rmeta.getResourceId(), (i + 1)));
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
                    boolean scaled = false;
                    int tries = 0;
                    while (!scaled && tries < 5) {
                        try {
                            tries++;
                            ResourceMetadata rm2 = svc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
                            rm2.setType(ResourceTypes.ThumbNail);
                            svc.updateResourceMetadata(env.obtainAccessToken(), rm2);
                            scaled = true;
                        } catch (ClientResponseFailure ex) {
                            if (ex.getResponse().getStatus() == 404) {
                                listener.reportNonFatalError(String.format("Unable to scale image \"%s\".  S3 is probably still injesting.  Waiting a few seconds for backend to catch up.", rmeta.getResourceId()));
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException ie) {
                                    //Do nothing.
                                }
                            } else {
                                throw ex;
                            }
                        }
                    }
                    if (!scaled) {
                        throw new IOException(String.format("Unable to scale the image \"%s\" after %d attempts.", rmeta.getResourceId(), retryCount));
                    }
                }
                if (ResourceTypes.DroneInspectionImage.equals(rmeta.getType())) {
                    // Queue the image to be zoomified.
                    rmeta.setStatus(ResourceStatus.Processed);
                } else {
                    // Mark the resource ready for user consumption
                    rmeta.setStatus(ResourceStatus.Released);
                }
                svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                success = true;
            } catch (IOException | URISyntaxException ex) {
                listener.reportNonFatalException("Error uploading resource.", ex);
            }
        }
    }

    private static void _uploadZoomify(Environment env, ResourceWebService svc, ProcessListener listener, String zoomifyId, File dataFile, int retryCount) {
        Boolean b;
        boolean success = false;
        for (int i = 0; (!success) && i < retryCount; i++) {
            try {
                success = false;
                listener.reportMessage(String.format("Uploading zoomify file \"%s\" for resource \"%s\", attempt %d", dataFile, zoomifyId, (i + 1)));
                HttpClientUtilities.postFile(env, zoomifyId, "image/zif", dataFile);
                success = true;
            } catch (IOException | URISyntaxException ex) {
                listener.reportNonFatalException("Error uploading resource.", ex);
            }
        }
        if (!success) {
            listener.reportNonFatalError(String.format("Unable upload the zoomify image \"%s\" after %d attempts.", zoomifyId, retryCount));
        }
    }
}
