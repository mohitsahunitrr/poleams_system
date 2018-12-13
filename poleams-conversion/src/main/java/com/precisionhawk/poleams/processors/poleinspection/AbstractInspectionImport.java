package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pchapman
 */
public abstract class AbstractInspectionImport {
    
    public static void savePoleData(Environment env, ImportProcessListener listener, InspectionData data) throws IOException {
        // Save SubStation
        if (!data.getSubStationsByFeederId().isEmpty()) {
            SubStationWebService sssvc = env.obtainWebService(SubStationWebService.class);
            for (SubStation subStation : data.getSubStationsByFeederId().values()) {
                if (data.getDomainObjectIsNew().get(subStation.getId())) {
                    sssvc.create(env.obtainAccessToken(), subStation);
                    listener.reportMessage(String.format("Inserted new sub station %s", subStation.getFeederNumber()));
                } else {
                    sssvc.update(env.obtainAccessToken(), subStation);
                    listener.reportMessage(String.format("Updating sub station %s", subStation.getFeederNumber()));
                }
            }
        }

        // Save Poles
        if (!data.getPoleDataByFPLId().isEmpty()) {
            PoleWebService psvc = env.obtainWebService(PoleWebService.class);
            for (Pole pdata : data.getPoleDataByFPLId().values()) {
                if (data.getDomainObjectIsNew().get(pdata.getId())) {
                    psvc.create(env.obtainAccessToken(), pdata);
                    listener.reportMessage(String.format("Inserted new pole %s FPL ID %s", pdata.getId(), pdata.getFPLId()));
                } else {
                    psvc.update(env.obtainAccessToken(), pdata);
                    listener.reportMessage(String.format("Updated pole %s FPL ID %s", pdata.getId(), pdata.getFPLId()));
                }
            }
        }

        // Save Pole Inspections
        if (!data.getPoleInspectionsByFPLId().isEmpty()) {
            PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
            for (PoleInspection pi : data.getPoleInspectionsByFPLId().values()) {
                if (data.getDomainObjectIsNew().get(pi.getId())) {
                    pisvc.create(env.obtainAccessToken(), pi);
                    listener.reportMessage(String.format("Inserted new inspection for pole %s", pi.getPoleId()));
                } else {
                    pisvc.update(env.obtainAccessToken(), pi);
                    listener.reportMessage(String.format("updated inspection for pole %s", pi.getPoleId()));
                }
            }
        }
    }
    
    public static void saveAndUploadResources(Environment env, ImportProcessListener listener, InspectionData data) throws IOException {
            listener.reportMessage("Uploading SubStation resources.");
            ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
            for (ResourceMetadata rmeta : data.getSubStationResources()) {
                ResourceDataUploader.uploadResources(env, listener, data, data.getSubStationResources(), data.getResourceDataFiles(), 2);
            }
            int index = 1;
            for (List<ResourceMetadata> list : data.getPoleResources().values()) {
                listener.reportMessage(String.format("Uploading Pole resources ( %d of %d poles).", index++, data.getPoleResources().size()));
                ResourceDataUploader.uploadResources(env, listener, data, list, data.getResourceDataFiles(), 2);
            }
    }
}
