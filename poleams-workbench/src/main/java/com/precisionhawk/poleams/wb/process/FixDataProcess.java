package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String COMMAND = "fixData";
    
    // Fix inspection data for Line 10
    private boolean dry = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_DRY.equals(arg)) {
            if (dry) {
                // Only pass the arg once
                return false;
            } else {
                dry = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber("AA0954D8");
        try {
            WorkOrder wo = svcs.workOrders().retrieveById(svcs.token(), data.getCurrentOrderNumber());
            if (wo == null) {
                System.err.printf("Unable to load work order %s\n", data.getCurrentOrderNumber());
                return true;
            }
            data.addWorkOrder(wo, false);
            data.setCurrentWorkOrder(wo);
            for (String feederId : wo.getSiteIds()) {
                processFeeder(svcs, data, feederId);
            }
            ProcessListener listener = new CLIProcessListener();
            DataImportUtilities.saveData(svcs, listener, data);
            DataImportUtilities.saveResources(svcs, listener, data);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {}

    private void processFeeder(WSClientHelper svcs, InspectionData data, String feederId) throws IOException {
        Feeder feeder = svcs.feeders().retrieve(svcs.token(), feederId);
        if (feeder == null) {
            System.err.printf("Unable to load feeder %s\n", feederId);
        }
        System.out.printf("Feeder %s:%s\n", feeder.getName(), feederId);
        data.addFeeder(feeder, false);
        data.setCurrentFeeder(feeder);
        SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
        siparams.setOrderNumber(data.getCurrentOrderNumber());
        siparams.setSiteId(feederId);
        List<FeederInspection> inspections = svcs.feederInspections().search(svcs.token(), siparams);
        if (inspections.size() != 1) {
            String err = String.format("Wrong number of feeder inspections for feeder %s. Expected 1, got %d", feederId, inspections.size());
            throw new IOException(err);
        }
        FeederInspection insp = inspections.get(0);
        data.addFeederInspection(insp, false);
        data.setCurrentFeederInspection(insp);
        System.out.printf("Feeder %s:%s\tFeeder Insp%s\n", data.getCurrentFeeder().getName(), insp.getSiteId(), insp.getId());
        
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSiteId(feederId);
        for (Pole p : svcs.poles().search(svcs.token(), pparams)) {
            processPole(svcs, data, p);
        }
    }

    private void processPole(WSClientHelper svcs, InspectionData data, Pole p) throws IOException {
        System.out.printf("Feeder %s:%s\tFeeder Insp %s\tPole %s:%s\n", data.getCurrentFeeder().getName(), p.getSiteId(), data.getCurrentFeederInspection().getId(), p.getName(), p.getId());
        AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
        aiparams.setAssetId(p.getId());
        aiparams.setSiteInspectionId(data.getCurrentFeederInspection().getId());
        List<PoleInspection> inspections = svcs.poleInspections().search(svcs.token(), aiparams);
        if (inspections.size() != 1) {
            String err = String.format("Wrong number of pole inspections for feeder %s Pole %s: %s. Expected 1, got %d", data.getCurrentFeeder().getName(), p.getId(), p.getUtilityId(), inspections.size());
            throw new IOException(err);
        }
        PoleInspection insp = inspections.get(0);
        insp.setOrderNumber(data.getCurrentOrderNumber());
        data.addPoleInspection(p, insp, false);
        System.out.printf("Feeder %s:%s\tFeeder Insp %s\tPole %s:%s\tPole Insp %s\n", data.getCurrentFeeder().getName(), insp.getSiteId(), insp.getSiteInspectionId(), p.getName(), insp.getAssetId(), insp.getId());
        
        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setAssetId(p.getId());
        rparams.setOrderNumber(data.getCurrentOrderNumber());
        for (ResourceMetadata rmeta : svcs.resources().search(svcs.token(), rparams)) {
            rmeta.setAssetInspectionId(insp.getId());
            rmeta.setSiteInspectionId(insp.getSiteInspectionId());
            data.addResourceMetadata(rmeta, null, false);
            System.out.printf("Feeder %s:%s\tFeeder Insp %s\tPole %s:%s\tPole Insp %s\tResource %s\n", data.getCurrentFeeder().getName(), rmeta.getSiteId(), rmeta.getSiteInspectionId(), p.getName(), rmeta.getAssetId(), rmeta.getAssetInspectionId(), rmeta.getResourceId());
        }
    }
}
