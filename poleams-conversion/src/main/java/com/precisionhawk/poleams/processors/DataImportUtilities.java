package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.domain.InspectionEventResource;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.webservices.InspectionEventResourceWebService;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.InspectionEvent;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.InspectionEventWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class DataImportUtilities {
    
    private DataImportUtilities() {}
    
    public static boolean saveData(WSClientHelper wsclient, ProcessListener listener, InspectionData data) throws IOException {
        Environment env = wsclient.getEnv();
        
        // Save Feeder
        if (data.getFeeder() != null) {
            FeederWebService sssvc = env.obtainWebService(FeederWebService.class);
            if (data.getDomainObjectIsNew().get(data.getFeeder().getId())) {
                sssvc.create(env.obtainAccessToken(), data.getFeeder());
                listener.reportMessage(String.format("Inserted new feeder %s", data.getFeeder().getFeederNumber()));
            } else {
                sssvc.update(env.obtainAccessToken(), data.getFeeder());
                listener.reportMessage(String.format("Updated feeder %s", data.getFeeder().getFeederNumber()));
            }
        }
        
        // Save Transmission Line
        if (data.getLine() != null) {
            TransmissionLineWebService svc = env.obtainWebService(TransmissionLineWebService.class);
            if (data.getDomainObjectIsNew().get(data.getLine().getId())) {
                svc.create(env.obtainAccessToken(), data.getLine());
                listener.reportMessage(String.format("Inserted new transmission line %s", data.getLine().getLineNumber()));
            } else {
                svc.update(env.obtainAccessToken(), data.getLine());
                listener.reportMessage(String.format("Updated transmission line %s", data.getLine().getLineNumber()));
            }
        }

        // Save Work Order
        if (data.getWorkOrder() != null) {
            WorkOrderWebService svc = env.obtainWebService(WorkOrderWebService.class);
            if (data.getDomainObjectIsNew().get(data.getWorkOrder().getOrderNumber())) {
                svc.create(env.obtainAccessToken(), data.getWorkOrder());
                listener.reportMessage(String.format("Inserted new work order %s", data.getWorkOrder().getOrderNumber()));
            } else {
                svc.update(env.obtainAccessToken(), data.getWorkOrder());
                listener.reportMessage(String.format("Updated work order %s", data.getWorkOrder().getOrderNumber()));
            }
        }

        // Save Feeder Inspection
        if (data.getFeederInspection() != null) {
            FeederInspectionWebService svc = env.obtainWebService(FeederInspectionWebService.class);
            if (data.getDomainObjectIsNew().get(data.getFeederInspection().getId())) {
                svc.create(env.obtainAccessToken(), data.getFeederInspection());
                listener.reportMessage(String.format("Inserted new feeder inspection %s", data.getFeederInspection().getId()));
            } else {
                svc.update(env.obtainAccessToken(), data.getFeederInspection());
                listener.reportMessage(String.format("Updated feeder inspection %s", data.getFeederInspection().getId()));
            }
        }
        
        // Save Line Inspection
        if (data.getLineInspection() != null) {
            TransmissionLineInspectionWebService svc = env.obtainWebService(TransmissionLineInspectionWebService.class);
            if (data.getDomainObjectIsNew().get(data.getLineInspection().getId())) {
                svc.create(env.obtainAccessToken(), data.getLineInspection());
                listener.reportMessage(String.format("Inserted new transmission line inspection %s", data.getLineInspection().getId()));
            } else {
                svc.update(env.obtainAccessToken(), data.getLineInspection());
                listener.reportMessage(String.format("Updated transmission line inspection %s", data.getLineInspection().getId()));
            }
        }

        // Save Poles
        if (!data.getPoleDataByFPLId().isEmpty()) {
            PoleWebService psvc = env.obtainWebService(PoleWebService.class);
            for (Pole pdata : data.getPoleDataByFPLId().values()) {
                if (data.getDomainObjectIsNew().get(pdata.getId())) {
                    psvc.create(env.obtainAccessToken(), pdata);
                    listener.reportMessage(String.format("Inserted new pole %s Utility ID %s", pdata.getId(), pdata.getUtilityId()));
                } else {
                    psvc.update(env.obtainAccessToken(), pdata);
                    listener.reportMessage(String.format("Updated pole %s Utility ID %s", pdata.getId(), pdata.getUtilityId()));
                }
            }
        }
        
        // Save Transmission Structures
        if (!data.getStructureDataByStructureNum().isEmpty()) {
            TransmissionStructureWebService svc = env.obtainWebService(TransmissionStructureWebService.class);
            for (TransmissionStructure struct : data.getStructureDataByStructureNum().values()) {
                if (data.getDomainObjectIsNew().get(struct.getId())) {
                    svc.create(env.obtainAccessToken(), struct);
                    listener.reportMessage(String.format("Inserted new transmission structure %s for structure number %s", struct.getId(), struct.getStructureNumber()));
                } else {
                    svc.update(env.obtainAccessToken(), struct);
                    listener.reportMessage(String.format("Updated transmission structure %s for structure number %s", struct.getId(), struct.getStructureNumber()));
                }
            }
        }

        // Save Pole Inspections
        if (!data.getPoleInspectionsByFPLId().isEmpty()) {
            PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
            for (PoleInspection pi : data.getPoleInspectionsByFPLId().values()) {
                if (data.getDomainObjectIsNew().get(pi.getId())) {
                    pisvc.create(env.obtainAccessToken(), pi);
                    listener.reportMessage(String.format("Inserted new inspection for pole %s", pi.getAssetId()));
                } else {
                    pisvc.update(env.obtainAccessToken(), pi);
                    listener.reportMessage(String.format("updated inspection for pole %s", pi.getAssetId()));
                }
            }
        }
        
        // Save Transmission Structure Inspections
        if (!data.getStructureInspectionsByStructureNum().isEmpty()) {
            TransmissionStructureInspectionWebService svc = env.obtainWebService(TransmissionStructureInspectionWebService.class);
            for (TransmissionStructureInspection insp : data.getStructureInspectionsByStructureNum().values()) {
                if (data.getDomainObjectIsNew().get(insp.getId())) {
                    svc.create(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Inserted new inspection for transmission structure %s", insp.getAssetId()));
                } else {
                    svc.update(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Updated inspection for transmission structure %s", insp.getAssetId()));
                }
            }
        }
        
        if (!data.getInspectionEvents().isEmpty()) {
            InspectionEventWebService svc = env.obtainWebService(InspectionEventWebService.class);
            for (InspectionEvent evt : data.getInspectionEvents().values()) {
                if (data.getDomainObjectIsNew().get(evt.getId())) {
                    svc.create(env.obtainAccessToken(), evt);
                    listener.reportMessage(String.format("Inserted new inspection event for transmission structure %s", evt.getAssetId()));
                } else {
                    svc.update(env.obtainAccessToken(), evt);
                    listener.reportMessage(String.format("Updated inspection event for transmission structure %s", evt.getAssetId()));
                }
            }
        }
        
        if (!data.getInspectionEventResources().isEmpty()) {
            InspectionEventResourceWebService svc = env.obtainWebService(InspectionEventResourceWebService.class);
            for (InspectionEventResource res : data.getInspectionEventResources().values()) {
                if (data.getDomainObjectIsNew().get(res.getId())) {
                    svc.create(env.obtainAccessToken(), res);
                    listener.reportMessage(String.format("Inserted new inspection event resource for transmission structure %s", res.getAssetId()));
                } else {
                    svc.update(env.obtainAccessToken(), res);
                    listener.reportMessage(String.format("Updated inspection event resource for transmission structure %s", res.getAssetId()));
                }
            }
        }
        
        return true;
    }
    
    public static boolean saveResources(WSClientHelper wsclient, ProcessListener listener, InspectionData data) throws IOException {
        Environment env = wsclient.getEnv();
        ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
        for (ResourceMetadata rmeta : data.getSiteResources()) {
            ResourceDataUploader.uploadResources(env, listener, data, data.getSiteResources(), data.getResourceDataFiles(), 2);
        }
        int index = 1;
        for (List<ResourceMetadata> list : data.getAssetResources().values()) {
            listener.reportMessage(String.format("Uploading Asset resources ( %d of %d assets).", index++, data.getAssetResources().size()));
            ResourceDataUploader.uploadResources(env, listener, data, list, data.getResourceDataFiles(), 2);
        }
        return true;
    }
}
