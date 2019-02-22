package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.Site;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author pchapman
 */
public class DeleteSiteProcess extends ServiceClientCommandProcess {

    private static final String ARG_DEL_SITE = "-a";
    private static final String ARG_SITE_ID = "-s";
    private static final String COMMAND = "deleteSite";

    private boolean all = false;
    private String siteId;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_DEL_SITE:
                all = true;
                return true;
            case ARG_SITE_ID:
                if (siteId == null) {
                    siteId = args.poll();
                    return siteId != null;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (siteId == null) {
            return false;
        }
        WSClientHelper services = new WSClientHelper(env);
        try {
            boolean success = executeFeeder(services);
            success = success | executeTransmissionLine(services);
            if (success) {
                deleteWorkOrders(services, siteId);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }
    
    protected boolean executeFeeder(WSClientHelper services) throws IOException {
        try {
            Feeder s = services.feeders().retrieve(services.token(), siteId);
            if (s != null) {
                deleteResources(services, s);
                deletePoleInspections(services, s);
                deletePoles(services, s);
                
                // Just in case
                deleteTransmissionStructureInspections(services, s);
                deleteTransmissionStructures(services, s);
                
                if (all) {
                    deletePoleInspections(services, s);
                    deleteFeederInspections(services, s);
                    services.feeders().delete(services.token(), siteId);
                }
                
                return true;
            }
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                System.out.printf("No Feeder with ID %s found.\n", siteId);
            } else {
                throw new IOException(ex);
            }
        }
        return false;
    }
    
    protected boolean executeTransmissionLine(WSClientHelper services) throws IOException {
        try {
            TransmissionLine s = services.transmissionLines().retrieve(services.token(), siteId);
            if (s != null) {
                deleteResources(services, s);
                deleteTransmissionStructureInspections(services, s);
                deleteTransmissionStructures(services, s);

                // Just in case
                deletePoleInspections(services, s);
                deletePoles(services, s);  
                
                if (all) {
                    deleteFeederInspections(services, s);
                    deletePoleInspections(services, s);
                    services.feeders().delete(services.token(), siteId);
                }
                
                return true;
            }
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                System.out.printf("No Feeder with ID %s found.\n", siteId);
            } else {
                throw new IOException(ex);
            }
        }
        return false;
    }
    
    private void deleteFeederInspections(WSClientHelper services, Site site) throws IOException {
        FeederInspectionWebService svc = services.feederInspections();
        SiteInspectionSearchParams params = new SiteInspectionSearchParams();
        params.setSiteId(site.getId());
        for (FeederInspection insp : svc.search(services.token(), params)) {
            svc.delete(services.token(), insp.getId());
            System.out.printf("Site Inspection %s deleted.\n", insp.getId());
        }
    }
    
    
    private void deletePoles(WSClientHelper services, Site site) throws IOException {
        PoleWebService svc = services.poles();
        PoleSearchParams params = new PoleSearchParams();
        params.setSiteId(site.getId());
        for (Pole a : svc.search(services.token(), params)) {
            svc.delete(services.token(), a.getId());
            System.out.printf("Pole %s deleted.\n", a.getId());
        }
    }
    
    private void deletePoleInspections(WSClientHelper services, Site site) throws IOException {
        PoleInspectionWebService svc = services.poleInspections();
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setSiteId(site.getId());
        for (PoleInspection insp : svc.search(services.token(), params)) {
            svc.delete(services.token(), insp.getId());
            System.out.printf("Pole Inspection %s deleted.\n", insp.getId());
        }
    }
    
    private void deleteTransmissionLineInspections(WSClientHelper services, Site site) throws IOException {
        TransmissionLineInspectionWebService svc = services.transmissionLineInspections();
        SiteInspectionSearchParams params = new SiteInspectionSearchParams();
        params.setSiteId(site.getId());
        for (TransmissionLineInspection insp : svc.search(services.token(), params)) {
            svc.delete(services.token(), insp.getId());
            System.out.printf("Transmission Line Inspection %s deleted.\n", insp.getId());
        }
    }
    
    private void deleteTransmissionStructures(WSClientHelper services, Site site) throws IOException {
        TransmissionStructureWebService svc = services.transmissionStructures();
        TransmissionStructureSearchParams params = new TransmissionStructureSearchParams();
        params.setSiteId(site.getId());
        for (TransmissionStructure a : svc.search(services.token(), params)) {
            svc.delete(services.token(), a.getId());
            System.out.printf("Transmission Structure %s deleted.\n", a.getId());
        }
    }
    
    private void deleteTransmissionStructureInspections(WSClientHelper services, Site site) throws IOException {
        TransmissionStructureInspectionWebService svc = services.transmissionStructureInspections();
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setSiteId(site.getId());
        for (TransmissionStructureInspection insp : svc.search(services.token(), params)) {
            svc.delete(services.token(), insp.getId());
            System.out.printf("Transmission Structure Inspection %s deleted.\n", insp.getId());
        }
    }
    
    private void deleteResources(WSClientHelper services, Site site) throws IOException {
        ResourceWebService svc = services.resources();
        ResourceSearchParams params = new ResourceSearchParams();
        params.setSiteId(site.getId());
        for (ResourceMetadata rmeta : svc.search(services.token(), params)) {
            svc.delete(services.token(), rmeta.getResourceId());
            System.out.printf("Resource %s deleted.\n", rmeta.getResourceId());
        }
    }
    
        
    private void deleteWorkOrders(WSClientHelper services, String siteId) throws IOException {
        WorkOrderWebService svc = services.workOrders();
        WorkOrderSearchParams params = new WorkOrderSearchParams();
        params.setSiteId(siteId);
        for (WorkOrder wo : svc.search(services.token(), params)) {
            wo.getSiteIds().remove(siteId);
            if (wo.getSiteIds().isEmpty()) {
                svc.delete(services.token(), wo.getOrderNumber());
                System.out.printf("Work Order %s deleted.\n", wo.getOrderNumber());
            } else {
                svc.update(services.token(), wo);
                System.out.printf("Work Order %s updated without site %s.\n", wo.getOrderNumber(), siteId);
            }
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
    }
    
}
