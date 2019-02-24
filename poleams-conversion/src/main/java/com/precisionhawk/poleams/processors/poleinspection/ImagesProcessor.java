package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.AssetInspection;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import java.io.File;
import java.io.FilenameFilter;
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
import org.apache.commons.imaging.common.IImageMetadata;
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

    private FilenameFilter droneImageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(DRONE_IMG);
        }
    };
    public FilenameFilter getDroneImageFilter() {
        return droneImageFilter;
    }
    public void setDroneImageFilter(FilenameFilter droneImageFilter) {
        this.droneImageFilter = droneImageFilter;
    }

    private FilenameFilter manualImageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(MANUAL_IMG_1) || name.toLowerCase().startsWith(MANUAL_IMG_2);
        }
    };
    public FilenameFilter getManualImageFilter() {
        return manualImageFilter;
    }
    public void setManualImageFilter(FilenameFilter manualImageFilter) {
        this.manualImageFilter = manualImageFilter;
    }

    private FilenameFilter thermalImageFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(THERMAL_IMG);
        }
    };
    public FilenameFilter getThermalImageFilter() {
        return thermalImageFilter;
    }
    public void setThermalImageFilter(FilenameFilter thermalImageFilter) {
        this.thermalImageFilter = thermalImageFilter;
    }

    void process(Environment environment, ProcessListener listener, InspectionData data, Pole p, File f, ImageFormat format)
        throws IOException, ImageReadException
    {
        
        ResourceWebService rsvc = environment.obtainWebService(ResourceWebService.class);
                
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetId(p.getId());
        params.setName(f.getName());
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.search(environment.obtainAccessToken(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            rmeta.setResourceId(UUID.randomUUID().toString());
            rmeta.setType(ResourceTypes.Other);
            if (getDroneImageFilter().accept(f.getParentFile(), f.getName())) {
                rmeta.setType(ResourceTypes.DroneInspectionImage);
            } else if (getManualImageFilter().accept(f.getParentFile(), f.getName())) {
                rmeta.setType(ResourceTypes.ManualInspectionImage);
            } else if (getThermalImageFilter().accept(f.getParentFile(), f.getName())) {
                rmeta.setType(ResourceTypes.Thermal);
            }
            listener.reportMessage(String.format("\tType %s determined for image file %s", rmeta.getType().getValue(), f.getName()));
            ImageInfo info = Imaging.getImageInfo(f);
            IImageMetadata metadata = Imaging.getMetadata(f);
            TiffImageMetadata exif = null;
            if (metadata instanceof JpegImageMetadata) {
                exif = ((JpegImageMetadata)metadata).getExif();
            } else if (metadata instanceof TiffImageMetadata) {
                exif = (TiffImageMetadata)metadata;
            } else {
                exif = null;
            }
            AssetInspection ai = data.getPoleInspectionsMap().get(new SiteAssetKey(p.getSiteId(), p.getUtilityId()));
            rmeta.setAssetId(p.getId());
            rmeta.setAssetInspectionId(ai.getId());
            rmeta.setContentType(info.getMimeType());
            rmeta.setLocation(ImageUtilities.getLocation(exif));
            rmeta.setName(f.getName());
            rmeta.setOrderNumber(ai.getOrderNumber());
            rmeta.setSize(ImageUtilities.getSize(info));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(p.getSiteId());
            rmeta.setSiteInspectionId(ai.getSiteInspectionId());
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
