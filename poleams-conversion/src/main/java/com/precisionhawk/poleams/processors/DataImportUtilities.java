package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.InspectionEventResource;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.domain.WorkOrderStatus;
import com.precisionhawk.ams.domain.WorkOrderType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.InspectionEventResourceWebService;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.InspectionEvent;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
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
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author pchapman
 */
public class DataImportUtilities {
    
    private DataImportUtilities() {}
    
    public static boolean saveData(WSClientHelper wsclient, ProcessListener listener, InspectionData data) throws IOException {
        Environment env = wsclient.getEnv();
        
        // Save Feeders
        {
            FeederWebService sssvc = env.obtainWebService(FeederWebService.class);
            if (data.getCurrentFeeder() != null && !data.getFeedersByFeederNum().keySet().contains(data.getCurrentFeeder().getFeederNumber())) {
                data.getFeedersByFeederNum().put(data.getCurrentFeeder().getFeederNumber(), data.getCurrentFeeder());
            }
            for (Feeder feeder : data.getFeedersByFeederNum().values()) {
                if (data.getDomainObjectIsNew().get(feeder.getId())) {
                    sssvc.create(env.obtainAccessToken(), feeder);
                    listener.reportMessage(String.format("Inserted new feeder %s", feeder.getFeederNumber()));
                } else {
                    sssvc.update(env.obtainAccessToken(), feeder);
                    listener.reportMessage(String.format("Updated feeder %s", feeder.getFeederNumber()));
                }
            }
        }
        
        // Save Transmission Line
        {
            TransmissionLineWebService svc = env.obtainWebService(TransmissionLineWebService.class);
            if (data.getCurrentLine() != null && !data.getLinesByLineNum().keySet().contains(data.getCurrentLine().getLineNumber())) {
                data.getLinesByLineNum().put(data.getCurrentLine().getLineNumber(), data.getCurrentLine());
            }
            for (TransmissionLine line : data.getLinesByLineNum().values()) {
                if (data.getDomainObjectIsNew().get(line.getId())) {
                    svc.create(env.obtainAccessToken(), line);
                    listener.reportMessage(String.format("Inserted new transmission line %s", line.getLineNumber()));
                } else {
                    svc.update(env.obtainAccessToken(), line);
                    listener.reportMessage(String.format("Updated transmission line %s", line.getLineNumber()));
                }
            }
        }

        // Save Work Order
        {
            WorkOrderWebService svc = env.obtainWebService(WorkOrderWebService.class);
            if (data.getCurrentWorkOrder() != null && !data.getWorkOrders().keySet().contains(data.getCurrentWorkOrder().getOrderNumber())) {
                data.getWorkOrders().put(data.getCurrentWorkOrder().getOrderNumber(), data.getCurrentWorkOrder());
            }
            for (WorkOrder wo : data.getWorkOrders().values()) {
                if (data.getDomainObjectIsNew().get(wo.getOrderNumber())) {
                    svc.create(env.obtainAccessToken(), wo);
                    listener.reportMessage(String.format("Inserted new work order %s", wo.getOrderNumber()));
                } else {
                    svc.update(env.obtainAccessToken(), wo);
                    listener.reportMessage(String.format("Updated work order %s", wo.getOrderNumber()));
                }
            }
        }

        // Save Feeder Inspection
        {
            FeederInspectionWebService svc = env.obtainWebService(FeederInspectionWebService.class);
            if (data.getCurrentFeederInspection() != null && !data.getFeederInspections().containsKey(data.getCurrentFeederInspection().getId())) {
                data.getFeederInspections().put(data.getCurrentFeederInspection().getId(), data.getCurrentFeederInspection());
            }
            for (FeederInspection insp : data.getFeederInspections().values()) {
                if (data.getDomainObjectIsNew().get(insp.getId())) {
                    svc.create(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Inserted new feeder inspection %s", insp.getId()));
                } else {
                    svc.update(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Updated feeder inspection %s", insp.getId()));
                }
            }
        }
        
        // Save Line Inspection
        {
            TransmissionLineInspectionWebService svc = env.obtainWebService(TransmissionLineInspectionWebService.class);
            if (data.getCurrentLineInspection() != null && !data.getLineInspections().containsKey(data.getCurrentLineInspection().getId())) {
                data.getLineInspections().put(data.getCurrentLineInspection().getId(), data.getCurrentLineInspection());
            }
            for (TransmissionLineInspection insp : data.getLineInspections().values()) {
                if (data.getDomainObjectIsNew().get(insp.getId())) {
                    svc.create(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Inserted new transmission line inspection %s", insp.getId()));
                } else {
                    svc.update(env.obtainAccessToken(), insp);
                    listener.reportMessage(String.format("Updated transmission line inspection %s", insp.getId()));
                }
            }
        }

        // Save Poles
        if (!data.getPolesMap().isEmpty()) {
            PoleWebService psvc = env.obtainWebService(PoleWebService.class);
            for (Pole pdata : data.getPolesMap().values()) {
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
        if (!data.getStructuresMap().isEmpty()) {
            TransmissionStructureWebService svc = env.obtainWebService(TransmissionStructureWebService.class);
            for (TransmissionStructure struct : data.getStructuresMap().values()) {
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
        if (!data.getPoleInspectionsMap().isEmpty()) {
            PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
            for (PoleInspection pi : data.getPoleInspectionsMap().values()) {
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
        if (!data.getStructureInspectionsMap().isEmpty()) {
            TransmissionStructureInspectionWebService svc = env.obtainWebService(TransmissionStructureInspectionWebService.class);
            for (TransmissionStructureInspection insp : data.getStructureInspectionsMap().values()) {
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

    public static boolean ensureFeeder(WSClientHelper svcs, InspectionData data, ProcessListener listener, String feederNum, String subStationName, String inspectionStatus) throws IOException {
        Feeder f = data.getFeedersByFeederNum().get(feederNum);
        if (f == null) {
            FeederSearchParams params = new FeederSearchParams();
            params.setOrganizationId(data.getOrganizationId());
            params.setFeederNumber(feederNum);
            f = CollectionsUtilities.firstItemIn(svcs.feeders().search(svcs.token(), params));
            if (f == null) {
                if (subStationName == null) {
                    listener.reportMessage(String.format("Cannot create a feeder %s with null name.", feederNum));
                    return false;
                }
                f = new Feeder();
                f.setFeederNumber(feederNum);
                f.setName(subStationName);
                f.setOrganizationId(data.getOrganizationId());
                f.setId(UUID.randomUUID().toString());
                data.addFeeder(f, true);
            } else {
                data.addFeeder(f, false);
            }
        }
        data.setCurrentFeeder(f);
        
        if (data.getCurrentWorkOrder() != null) {
            boolean found = false;
            for (String siteId : data.getCurrentWorkOrder().getSiteIds()) {
                if (f.getId().equals(siteId)) {
                    found = true;
                }
            }
            if (!found) {
                data.getCurrentWorkOrder().getSiteIds().add(f.getId());
            }

            FeederInspection fi = null;
            for (FeederInspection insp : data.getFeederInspections().values()) {
                if (insp.getSiteId().equals(f.getId())) {
                    fi = insp;
                    break;
                }
            }

            if (fi == null) {
                SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
                siparams.setSiteId(f.getId());
                siparams.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                fi = CollectionsUtilities.firstItemIn(svcs.feederInspections().search(svcs.token(), siparams));
                if (fi == null) {
                    fi = new FeederInspection();
                    fi.setDateOfInspection(LocalDate.now());
                    fi.setId(UUID.randomUUID().toString());
                    fi.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                    fi.setSiteId(siparams.getSiteId());
                    fi.setStatus(new SiteInspectionStatus(inspectionStatus));
                    fi.setType(new SiteInspectionType(data.getCurrentWorkOrder().getType().getValue()));
                    data.addFeederInspection(fi, true);
                } else {
                    data.addFeederInspection(fi, false);
                }
            }
            data.setCurrentFeederInspection(fi);
        }
        
        return true;
    }

    public static boolean ensureLine(WSClientHelper svcs, InspectionData data, ProcessListener listener, String lineNum, String lineName) throws IOException {
        TransmissionLine tl = data.getLinesByLineNum().get(lineNum);
        if (tl == null) {
            TransmissionLineSearchParams params = new TransmissionLineSearchParams();
            params.setOrganizationId(data.getOrganizationId());
            params.setLineNumber(lineNum);
            tl = CollectionsUtilities.firstItemIn(svcs.transmissionLines().search(svcs.token(), params));
            if (tl == null) {
                tl = new TransmissionLine();
                tl.setLineNumber(lineNum);
                tl.setName(lineName);
                tl.setOrganizationId(data.getOrganizationId());
                tl.setId(UUID.randomUUID().toString());
                data.addLine(tl, true);
            } else {
                data.addLine(tl, false);
            }

            if (data.getCurrentWorkOrder() != null) {
                boolean found = false;
                for (String siteId : data.getCurrentWorkOrder().getSiteIds()) {
                    if (tl.getId().equals(siteId)) {
                        found = true;
                    }
                }
                if (!found) {
                    data.getCurrentWorkOrder().getSiteIds().add(tl.getId());
                }
            }
        }

        TransmissionLineInspection tli = null;
        for (TransmissionLineInspection insp : data.getLineInspections().values()) {
            if (insp.getSiteId().equals(tl.getId())) {
                tli = insp;
                break;
            }
        }
        
        if (tli == null) {
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setSiteId(tl.getId());
            siparams.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
            tli = CollectionsUtilities.firstItemIn(svcs.transmissionLineInspections().search(svcs.token(), siparams));
            if (tli == null) {
                tli = new TransmissionLineInspection();
                tli.setDateOfInspection(LocalDate.now());
                tli.setId(UUID.randomUUID().toString());
                tli.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                tli.setSiteId(siparams.getSiteId());
                tli.setStatus(new SiteInspectionStatus("Processed"));
                tli.setType(new SiteInspectionType(data.getCurrentWorkOrder().getType().getValue()));
                data.addLineInspection(tli, true);
            } else {
                data.addLineInspection(tli, false);
            }
        }

        data.setCurrentLine(tl);
        data.setCurrentLineInspection(tli);
        
        return true;
    }

    public static Pole ensurePole(WSClientHelper svcs, ProcessListener listener, InspectionData data, String fplid, LocalDate inspectionDate) throws IOException {
        
        Pole pole = data.getPolesMap().get(new SiteAssetKey(data.getCurrentFeeder().getId(), fplid));
        if (pole != null) {
            return pole;
        }
        
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSiteId(data.getCurrentFeeder().getId());
        pparams.setUtilityId(fplid);
        pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
        if (pole == null) {
            pole = new Pole();
            pole.setUtilityId(fplid);
            pole.setId(UUID.randomUUID().toString());
            pole.setSiteId(data.getCurrentFeeder().getId());
            data.addPole(pole, true);
        } else {
            data.addPole(pole, false);
        }
        ensurePoleInspection(svcs, listener, data, pole, inspectionDate);
        return pole;
    }

    public static PoleInspection ensurePoleInspection(
            WSClientHelper svcs, ProcessListener listener, InspectionData data, Pole pole, LocalDate inspectionDate
        ) throws IOException
    {
        PoleInspection insp = data.getPoleInspectionsMap().get(new SiteAssetKey(pole));
        if (insp == null) {
            AssetInspectionSearchParams iparams = new AssetInspectionSearchParams();
            iparams.setAssetId(pole.getId());
            iparams.setOrderNumber(data.getCurrentOrderNumber());
            insp = CollectionsUtilities.firstItemIn(svcs.poleInspections().search(svcs.token(), iparams));
            if (insp == null) {
                insp = new PoleInspection();
                insp.setAssetId(pole.getId());
                insp.setDateOfInspection(inspectionDate);
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                insp.setSiteId(data.getCurrentFeeder().getId());
                insp.setSiteInspectionId(data.getCurrentFeederInspection().getId());
                insp.setStatus(new AssetInspectionStatus("Processed"));
                insp.setType(new AssetInspectionType("DistributionLineInspection"));
                data.addPoleInspection(pole, insp, true);
            } else {
                insp.setType(new AssetInspectionType("DistributionLineInspection"));
                data.addPoleInspection(pole, insp, false);
            }
        }
        return insp;
    }
    
//    public static Resource ensureResource(WSClientHelper svcs, ProcessListener listener, InspectionData data, )

    public static TransmissionStructure ensureTxStructure(WSClientHelper svcs, ProcessListener listener, InspectionData data, String utilityid, LocalDate inspectionDate) throws IOException {
        
        TransmissionStructure ts = data.getStructuresMap().get(new SiteAssetKey(data.getCurrentLine().getId(), utilityid));
        if (ts != null) {
            return ts;
        }
        
        TransmissionStructureSearchParams pparams = new TransmissionStructureSearchParams();
        pparams.setSiteId(data.getCurrentFeeder().getId());
        pparams.setStructureNumber(utilityid);
        ts = CollectionsUtilities.firstItemIn(svcs.transmissionStructures().search(svcs.token(), pparams));
        if (ts == null) {
            ts = new TransmissionStructure();
            ts.setStructureNumber(utilityid);
            ts.setId(UUID.randomUUID().toString());
            ts.setSiteId(data.getCurrentLine().getId());
            ts.setName(utilityid);
            data.addTransmissionStruture(ts, true);
        } else {
            data.addTransmissionStruture(ts, false);
        }
        ensureTransmissionStructureInspection(svcs, listener, data, ts, inspectionDate);
        return ts;
    }

    public static TransmissionStructureInspection ensureTransmissionStructureInspection(
            WSClientHelper svcs, ProcessListener listener, InspectionData data, TransmissionStructure struct, LocalDate inspectionDate
        ) throws IOException
    {
        TransmissionStructureInspection insp = data.getStructureInspectionsMap().get(new SiteAssetKey(struct));
        if (insp == null) {
            AssetInspectionSearchParams iparams = new AssetInspectionSearchParams();
            iparams.setAssetId(struct.getId());
            iparams.setOrderNumber(data.getCurrentOrderNumber());
            insp = CollectionsUtilities.firstItemIn(svcs.transmissionStructureInspections().search(svcs.token(), iparams));
            if (insp == null) {
                insp = new TransmissionStructureInspection();
                insp.setAssetId(struct.getId());
                insp.setDateOfInspection(inspectionDate);
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                insp.setSiteId(data.getCurrentLine().getId());
                insp.setSiteInspectionId(data.getCurrentLineInspection().getId());
                insp.setStatus(new AssetInspectionStatus("Processed"));
                insp.setType(new AssetInspectionType("TransmissionLineInspection"));
                data.addTransmissionStructureInspection(struct, insp, true);
            } else {
                insp.setType(new AssetInspectionType("TransmissionLineInspection"));
                data.addTransmissionStructureInspection(struct, insp, false);
            }
        }
        return insp;
    }

    public static boolean ensureWorkOrder(WSClientHelper svcs, InspectionData data, ProcessListener listener, WorkOrderType type) throws IOException {
        try {
            data.setCurrentWorkOrder(svcs.workOrders().retrieveById(svcs.token(), data.getCurrentOrderNumber()));
        } catch (ClientResponseFailure f) {
            if (f.getResponse().getStatus() != HttpStatus.SC_NOT_FOUND) {
                // 404 is ok
                throw new IOException(f);
            }
        }
        if (data.getCurrentWorkOrder() == null) {
            WorkOrder wo = new WorkOrder();
            wo.setOrderNumber(data.getCurrentOrderNumber());
            wo.setRequestDate(LocalDate.now());
            wo.setStatus(new WorkOrderStatus("Requested"));
            wo.setType(type);
            data.setCurrentWorkOrder(wo);
            data.addWorkOrder(wo, true);
        } else {
            data.addWorkOrder(data.getCurrentWorkOrder(), false);
        }
        return true;
    }
    
    public static void addNonImageResource(
            WSClientHelper svcs, InspectionData data, FeederInspection finsp, PoleInspection pinsp, ResourceType rtype, File file, String contentType
        ) throws IOException
    {
        ResourceSearchParams params = new ResourceSearchParams();
        if (pinsp != null) {
            params.setAssetInspectionId(pinsp.getId());
        } else {
            params.setSiteInspectionId(finsp.getId());
        }
        params.setType(rtype);
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(svcs.resources().search(svcs.token(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            if (pinsp != null) {
                rmeta.setAssetId(pinsp.getAssetId());
                rmeta.setAssetInspectionId(pinsp.getId());
                rmeta.setSiteId(pinsp.getSiteId());
                rmeta.setSiteInspectionId(pinsp.getId());
            } else {
                rmeta.setSiteId(finsp.getSiteId());
                rmeta.setSiteInspectionId(finsp.getId());
            }
            rmeta.setContentType(contentType);
            rmeta.setName(file.getName());
            rmeta.setOrderNumber(data.getCurrentOrderNumber());
            rmeta.setResourceId(UUID.randomUUID().toString());
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setTimestamp(ZonedDateTime.now());
            rmeta.setType(rtype);
            data.addResourceMetadata(rmeta, file, true);
        } else {
            data.addResourceMetadata(rmeta, file, false);
        }
    }
}
