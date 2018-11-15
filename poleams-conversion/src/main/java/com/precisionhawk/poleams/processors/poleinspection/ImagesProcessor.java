package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

/**
 *
 * @author Philip A. Chapman
 */
//TODO: handle identified components images.  Their names look like:  rgb_DJI_3949917_ML1.jpg identified by the "rgb_DJI" and the "ML" portions of the name.
final class ImagesProcessor implements Constants {

    private static final String DRONE_IMG = "rgb";
    private static final String MANUAL_IMG_1 = "phone";
    private static final String MANUAL_IMG_2 = "IMG_";
    private static final String THERMAL_IMG = "thermal";
    //FIXME: We need a better way
    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/New_York");

    private ImagesProcessor() {}

    static void process(Environment environment, ProcessListener listener, InspectionData data, Pole p, File f, ImageFormat format)
        throws IOException, ImageReadException
    {
        
        ResourceWebService rsvc = environment.obtainWebService(ResourceWebService.class);
                
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetId(p.getId());
        params.setName(f.getName());
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.query(environment.obtainAccessToken(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            rmeta.setResourceId(UUID.randomUUID().toString());
            String name = f.getName().toLowerCase();
            rmeta.setType(ResourceTypes.Other);
            if (name.startsWith(DRONE_IMG)) {
                rmeta.setType(ResourceTypes.DroneInspectionImage);
            } else if (name.startsWith(MANUAL_IMG_1) || name.toLowerCase().startsWith(MANUAL_IMG_2)) {
                rmeta.setType(ResourceTypes.ManualInspectionImage);
            } else if (name.startsWith(THERMAL_IMG)) {
                rmeta.setType(ResourceTypes.Thermal);
            }
            ImageInfo info = Imaging.getImageInfo(f);
            ImageMetadata metadata = Imaging.getMetadata(f);
            TiffImageMetadata exif = null;
            if (metadata instanceof JpegImageMetadata) {
                exif = ((JpegImageMetadata)metadata).getExif();
            } else if (metadata instanceof TiffImageMetadata) {
                exif = (TiffImageMetadata)metadata;
            } else {
                exif = null;
            }
            rmeta.setContentType(info.getMimeType());
            rmeta.setLocation(ImageUtilities.getLocation(exif));
            rmeta.setName(f.getName());
            rmeta.setOrderNumber(data.getOrderNumber());
            rmeta.setAssetId(p.getId());
            rmeta.setAssetInspectionId(data.getPoleInspectionsByFPLId().get(p.getUtilityId()).getId());
            rmeta.setSize(ImageUtilities.getSize(info));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(data.getFeeder().getId());
            rmeta.setTimestamp(ImageUtilities.getTimestamp(exif, DEFAULT_TZ));
            data.addResourceMetadata(rmeta, f, true);
        } else {
            List<String> resourceIDs = new LinkedList<>();
            resourceIDs.add(rmeta.getResourceId());
            Map<String, Boolean> results = rsvc.verifyUploadedResources(environment.obtainAccessToken(), resourceIDs);
            if (
                    rmeta.getStatus() == ResourceStatus.QueuedForUpload
                    || (!results.get(rmeta.getResourceId()))
                )
            {
                // Add it to the list so that the upload is attempted again
                data.addResourceMetadata(rmeta, f, false);
            }
        }
    }
    
}
