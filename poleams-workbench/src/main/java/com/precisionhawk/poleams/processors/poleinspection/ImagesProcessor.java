package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.imaging.ImageFormat;

/**
 *
 * @author pchapman
 */
final class ImagesProcessor implements Constants {

    private static final String DRONE_IMG = "rgb";
    private static final String MANUAL_IMG = "phone";
    private static final String THERMAL_IMG = "thermal";

    private ImagesProcessor() {}

    static void process(Environment environment, ProcessListener listener, InspectionData data, Pole p, File f, ImageFormat format) throws IOException {
        
        ResourceWebService rsvc = environment.obtainWebService(ResourceWebService.class);
                
        ResourceSearchParameters params = new ResourceSearchParameters();
        params.setPoleId(p.getId());
        params.setName(f.getName());
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.query(environment.obtainAccessToken(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            String name = f.getName().toLowerCase();
            ResourceType rtype = ResourceType.Other;
            if (name.startsWith(DRONE_IMG)) {
                rtype = ResourceType.DroneInspectionImage;
            } else if (name.startsWith(MANUAL_IMG)) {
                rtype = ResourceType.ManualInspectionImage;
            } else if (name.startsWith(THERMAL_IMG)) {
                rtype = ResourceType.Thermal;
            }
            rmeta.setContentType(ImageUtilities.ImageType.valueOf(format.getName()).getContentType());
            rmeta.setLocation(ImageUtilities.getLocation(f));
            rmeta.setName(f.getName());
            rmeta.setOrganizationId(ORG_ID);
            rmeta.setPoleId(p.getId());
            rmeta.setSize(ImageUtilities.getSize(f));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSubStationId(data.getSubStation().getId());
            rmeta.setTimestamp(ImageUtilities.getTimestamp(metadata));
            data.addResourceMetadata(rmeta, true);
        } else {
            List<String> resourceIDs = new LinkedList<>();
            resourceIDs.add(rmeta.getResourceId());
            Map<String, Boolean> results = rsvc.verifyUploadedResources(environment.obtainAccessToken(), resourceIDs);
            if (rmeta.getStatus() == ResourceStatus.QueuedForUpload) {
                // Add it to the list so that the upload is attempted again
                data.addResourceMetadata(rmeta, false);
            }
        }
    }
    
}
