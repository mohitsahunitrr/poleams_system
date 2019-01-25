package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.InspectionEventResourceSearchParams;
import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.InspectionEventResource;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.ResourceSummary;
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
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author pchapman
 */
public class DataCopyProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_ORDER_NUM = "-wo";
    private static final String ARG_SOURCE_ENV = "-source";
    private static final String COMMAND = "copyData";
    private static final String HELP = "\t" + COMMAND + " " + ARGS_FOR_HELP + " " + ARG_SOURCE_ENV + " SourceEnvironment " + ARG_ORDER_NUM + " WorkOrderNumber";

    private String sourceEnvName;
    private String orderNum;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_ORDER_NUM:
                if (orderNum == null) {
                    orderNum = args.poll();
                    return orderNum != null;
                } else {
                    return false;
                }
            case ARG_SOURCE_ENV:
                if (sourceEnvName == null) {
                    sourceEnvName = args.poll();
                    return sourceEnvName != null;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment targetEnv) {
        if (sourceEnvName == null || orderNum == null) {
            return false;
        }
        
        Environment srcEnv = null;
        for (Environment e : environments) {
            if (sourceEnvName.equals(e.getName())) {
                srcEnv = e;
                break;
            }
        }
        if (srcEnv == null) {
            System.err.printf("Unable to locate configuration for environment %s\n", sourceEnvName);
            return false;
        }
        
        WSClientHelper sServices = new WSClientHelper(srcEnv);
        WSClientHelper tServices = new WSClientHelper(targetEnv);
        
        try {
            copyWorkOrder(sServices, tServices);
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
    }

    private boolean copyWorkOrder(WSClientHelper sServices, WSClientHelper tServices) throws IOException, URISyntaxException {
        WorkOrder swo = null;
        try {
            swo = sServices.workOrders().retrieveById(sServices.token(), orderNum);
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup work order %s in source data.", orderNum), ex);
            }
        }
        if (swo == null) {
            System.err.printf("The work order %s could not be found in source data.", orderNum);
            return false;
        } else {
            WorkOrder two = null;
            try {
                two = tServices.workOrders().retrieveById(tServices.token(), orderNum);
            } catch (ClientResponseFailure ex) {
                if (ex.getResponse().getStatus() == 404) {
                    // it does not exist.
                } else {
                    throw new IOException(String.format("Unable to lookup work order %s in target data.", orderNum), ex);
                }
            }
            if (two == null) {
                two = swo;
                tServices.workOrders().create(tServices.token(), two);
            }
            if (two.getSiteIds().isEmpty()) {
                return false;
            }
            boolean success = true;
            for (String siteId : two.getSiteIds()) {
                success = success && processTransmissionLine(sServices, tServices, siteId);
                success = success && processFeeder(sServices, tServices, siteId);
            }
            return success;
        }
    }

    private boolean processTransmissionLine(WSClientHelper sServices, WSClientHelper tServices, String transmissionLineId)
        throws IOException, URISyntaxException
    {
        TransmissionLine sLine = null;
        try {
            sLine = sServices.transmissionLines().retrieve(sServices.token(), transmissionLineId);
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup transmission line %s in source data.", transmissionLineId), ex);
            }
        }
        if (sLine == null) {
            // It's probably a distribution line
            System.out.printf("Transmission line %s not found", transmissionLineId);
            return true;
        }
        TransmissionLine tLine = null;
        try {
            tLine = tServices.transmissionLines().retrieve(tServices.token(), sLine.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup transmission line %s in target data.", sLine.getId()), ex);
            }
        }
        if (tLine == null) {
            tLine = sLine;
            tServices.transmissionLines().create(tServices.token(), tLine);
            System.out.printf("Copied transmission line %s\n", tLine.getId());
        }
        
        SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
        siparams.setOrderNumber(orderNum);
        siparams.setSiteId(transmissionLineId);
        TransmissionLineInspection sInsp = CollectionsUtilities.firstItemIn(sServices.transmissionLineInspections().search(sServices.token(), siparams));
        if (sInsp == null) {
            System.err.printf("The transmission line inspections could not be found in source data for work order %s and line %s.", orderNum, transmissionLineId);
            return false;
        }
        TransmissionLineInspection tInsp = null;
        try {
            tInsp = tServices.transmissionLineInspections().retrieve(tServices.token(), sInsp.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup transmission line inspection %s. in target data", sInsp.getId()), ex);
            }
        }
        if (tInsp == null) {
            tInsp = sInsp;
            tServices.transmissionLineInspections().create(tServices.token(), tInsp);
            System.out.printf("Copied transmission line inspection %s\n", tInsp.getId());
        }
        
        TransmissionStructureSearchParams pparams = new TransmissionStructureSearchParams();
        pparams.setSiteId(transmissionLineId);
        for (TransmissionStructure sStruct : sServices.transmissionStructures().search(sServices.token(), pparams)) {
            if (!processTransmissionStructure(sServices, tServices, tInsp, sStruct)) {
                return false;
            }
        }
        
        InspectionEventSearchParams ieparams = new InspectionEventSearchParams();
        ieparams.setSiteId(sLine.getId());
        ieparams.setOrderNumber(orderNum);
        for (InspectionEvent ievt : sServices.inspectionEvents().search(sServices.token(), ieparams)) {
            if (!processInspectionEvent(sServices, tServices, ievt)) {
                return false;
            }
        }
        
        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setSiteId(sLine.getId());
        rparams.setOrderNumber(orderNum);
        for (ResourceSummary resourceSummary : sServices.resources().querySummaries(sServices.toString(), rparams)) {
            if (!processResource(sServices, tServices, tInsp.getId(), resourceSummary)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean processTransmissionStructure(WSClientHelper sServices, WSClientHelper tServices, TransmissionLineInspection insp, TransmissionStructure sStruct)
        throws IOException
    {
        TransmissionStructure tStruct = null;
        try {
            tStruct = tServices.transmissionStructures().retrieve(tServices.token(), sStruct.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup transmission structure %s. in target data", sStruct.getId()), ex);
            }
        }
        if (tStruct == null) {
            tStruct = sStruct;
            tServices.transmissionStructures().create(tServices.token(), tStruct);
            System.out.printf("Copied transmission structure %s\n", tStruct.getId());
        }
        
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setAssetId(sStruct.getId());
        params.setOrderNumber(orderNum);
        TransmissionStructureInspection sInsp = CollectionsUtilities.firstItemIn(sServices.transmissionStructureInspections().search(sServices.token(), params));
        if (sInsp == null) {
            System.err.printf("The transmission structure inspections could not be found in source data for work order %s and structure %s.", orderNum, sStruct.getId());
            return false;
        }
        TransmissionStructureInspection tInsp = null;
        try {
            tInsp = tServices.transmissionStructureInspections().retrieve(tServices.token(), sInsp.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup transmission structure inspection %s. in target data", sInsp.getId()), ex);
            }
        }
        if (tInsp == null) {
            tInsp = sInsp;
            tServices.transmissionStructureInspections().create(tServices.token(), tInsp);
            System.out.printf("Copied transmission structure inspection %s\n", tInsp.getId());
        }
        
        return true;
    }

    private boolean processResource(WSClientHelper sServices, WSClientHelper tServices, String siteInspectionId, ResourceSummary resourceSummary)
        throws IOException, URISyntaxException
    {
        // If it exists in target, no need to look up more info in source.
        ResourceMetadata tRMeta = null;
        try {
            tRMeta = tServices.resources().retrieve(tServices.token(), resourceSummary.getResourceId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup resource %s. in target data", resourceSummary.getResourceId()), ex);
            }
        }
        if (tRMeta != null) {
            if (tRMeta.getSiteInspectionId() == null) {
                tRMeta.setSiteInspectionId(siteInspectionId);
                tServices.resources().updateResourceMetadata(tServices.token(), tRMeta);
            }
        } else {
            // Copy the data
            ResourceMetadata sRMeta = null;
            try {
                sRMeta = sServices.resources().retrieve(sServices.token(), resourceSummary.getResourceId());
            } catch (ClientResponseFailure ex) {
                if (ex.getResponse().getStatus() == 404) {
                    // it does not exist.
                } else {
                    throw new IOException(String.format("Unable to lookup resource %s. in source data", resourceSummary.getResourceId()), ex);
                }
            }
            if (sRMeta == null) {
                System.err.printf("The resource %s could not be found in source data.", resourceSummary.getResourceId());
                return false;
            }
            tRMeta = sRMeta;
            if (tRMeta.getSiteInspectionId() == null) {
                tRMeta.setSiteInspectionId(siteInspectionId);
            }
            tRMeta.setStatus(ResourceStatus.QueuedForUpload);
            tServices.resources().insertResourceMetadata(tServices.token(), tRMeta);
            System.out.printf("Copied Resource %s\n", tRMeta.getResourceId());
        }
        
        // See if the resources already exist
        List<String> ids = new LinkedList<>();
        ids.add(tRMeta.getResourceId());
        if (tRMeta.getZoomifyId() != null) {
            ids.add(tRMeta.getZoomifyId());
        }
        Map<String, Boolean> uploaded = tServices.resources().verifyUploadedResources(tServices.token(), ids);
        
        // Upload resources, if they do not already exist
        processResourceData(tServices.getEnv(), tRMeta.getResourceId(), tRMeta.getContentType(), resourceSummary.getDownloadURL(), uploaded.get(tRMeta.getResourceId()));
        processResourceData(tServices.getEnv(), tRMeta.getZoomifyId(), "image/zif", resourceSummary.getZoomifyURL(), uploaded.get(tRMeta.getZoomifyId()));
        
        tRMeta.setStatus(ResourceStatus.Released);
        tServices.resources().updateResourceMetadata(tServices.token(), tRMeta);
        
        return true;
    }
    
    private void processResourceData(Environment env, String id, String contentType, String url, Boolean exists)
        throws IOException, URISyntaxException
    {
        if (id == null) {
            return;
        }
        if (exists == null || !exists) {
            File outFile = null;
            OutputStream os = null;
            InputStream is = null;
            try {
                // Download data
                outFile = new File(new File("/tmp/poleams/tmp"), id);
                os = new FileOutputStream(outFile);
                is = new URL(url).openStream();
                IOUtils.copy(is, os);
                // Upload data
                HttpClientUtilities.postFile(env, id, contentType, outFile);
                System.out.printf("Uploaded %s %s\n", contentType, id);
            } finally {
                // Clean up
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
                if (outFile != null) {
                    outFile.delete();
                }
            }
        }
    }

    private boolean processInspectionEvent(WSClientHelper sServices, WSClientHelper tServices,InspectionEvent sEvt)
        throws IOException
    {
        InspectionEvent tEvt = null;
        try {
            tEvt = tServices.inspectionEvents().retrieve(tServices.token(), sEvt.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup inspection event %s. in target data", sEvt.getId()), ex);
            }
        }
        if (tEvt == null) {
            tEvt = sEvt;
            tServices.inspectionEvents().create(tServices.token(), tEvt);
            System.out.printf("Copied inspection event %s\n", tEvt.getId());
        }
        
        InspectionEventResourceSearchParams params = new InspectionEventResourceSearchParams();
        params.setInspectionEventId(sEvt.getId());
        for (InspectionEventResource sRes : sServices.inspectionEventResources().search(sServices.token(), params)) {
            if (!processInspectionEventResource(sServices, tServices, sRes)) {
                return false;
            }
        }
        
        return true;
    }

    private boolean processInspectionEventResource(WSClientHelper sServices, WSClientHelper tServices, InspectionEventResource sRes)
        throws IOException
    {
        InspectionEventResource tRes = null;
        try {
            tRes = tServices.inspectionEventResources().retrieve(tServices.token(), sRes.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup inspection event resource %s. in target data", sRes.getId()), ex);
            }
        }
        if (tRes == null) {
            tRes = sRes;
            tServices.inspectionEventResources().create(tServices.token(), tRes);
            System.out.printf("Copied inspection event resource %s\n", tRes.getId());
        }
        return true;
    }
    
    private boolean processPole(WSClientHelper sServices, WSClientHelper tServices, FeederInspection insp, Pole sPole)
        throws IOException
    {
        Pole tPole = null;
        try {
            tPole = tServices.poles().retrieve(tServices.token(), sPole.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup pole %s. in target data", sPole.getId()), ex);
            }
        }
        if (tPole == null) {
            tPole = sPole;
            tServices.poles().create(tServices.token(), tPole);
            System.out.printf("Copied distribution Pole %s\n", tPole.getId());
        }
        
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setAssetId(tPole.getId());
        params.setOrderNumber(insp.getOrderNumber());
        PoleInspection sInsp = CollectionsUtilities.firstItemIn(sServices.poleInspections().search(sServices.token(), params));
        if (sInsp == null) {
            System.err.printf("The pole inspections could not be found in source data for work order %s and structure %s.", insp.getOrderNumber(), sPole.getId());
            return false;
        }
        PoleInspection tInsp = null;
        try {
            tInsp = tServices.poleInspections().retrieve(tServices.token(), sInsp.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup pole inspection %s. in target data", sInsp.getId()), ex);
            }
        }
        if (tInsp == null) {
            tInsp = sInsp;
            tServices.poleInspections().create(tServices.token(), tInsp);
            System.out.printf("Copied distribution Pole Inspection %s\n", tInsp.getId());
        }
        
        return true;
    }

    private boolean processFeeder(WSClientHelper sServices, WSClientHelper tServices, String feederId)
        throws IOException, URISyntaxException
    {
        Feeder sFeeder = null;
        try {
            sFeeder = sServices.feeders().retrieve(sServices.token(), feederId);
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup feeder %s in source data.", feederId), ex);
            }
        }
        if (sFeeder == null) {
            // It's probably a transmission line
            System.out.printf("Transmission line %s not found", feederId);
            return true;
        }
        Feeder tFeeder = null;
        try {
            tFeeder = tServices.feeders().retrieve(tServices.token(), sFeeder.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup feeder line %s in target data.", sFeeder.getId()), ex);
            }
        }
        if (tFeeder == null) {
            tFeeder = sFeeder;
            tServices.feeders().create(tServices.token(), tFeeder);
            System.out.printf("Copied distribution feeder %s\n", tFeeder.getId());
        }
        
        SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
        siparams.setOrderNumber(orderNum);
        siparams.setSiteId(feederId);
        FeederInspection sInsp = CollectionsUtilities.firstItemIn(sServices.feederInspections().search(sServices.token(), siparams));
        if (sInsp == null) {
            System.err.printf("The feeder inspections could not be found in source data for work order %s and feeder %s.", orderNum, feederId);
            return false;
        }
        FeederInspection tInsp = null;
        try {
            tInsp = tServices.feederInspections().retrieve(tServices.token(), sInsp.getId());
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException(String.format("Unable to lookup feeder inspection %s. in target data", sInsp.getId()), ex);
            }
        }
        if (tInsp == null) {
            tInsp = sInsp;
            tServices.feederInspections().create(tServices.token(), tInsp);
            System.out.printf("Copied distribution feeder inspection %s\n", tInsp.getId());
        }
        
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSiteId(feederId);
        for (Pole sPole : sServices.poles().search(sServices.token(), pparams)) {
            if (!processPole(sServices, tServices, tInsp, sPole)) {
                return false;
            }
        }
        
        InspectionEventSearchParams ieparams = new InspectionEventSearchParams();
        ieparams.setSiteId(sFeeder.getId());
        ieparams.setOrderNumber(orderNum);
        for (InspectionEvent ievt : sServices.inspectionEvents().search(sServices.token(), ieparams)) {
            if (!processInspectionEvent(sServices, tServices, ievt)) {
                return false;
            }
        }
        
        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setSiteId(feederId);
        rparams.setOrderNumber(orderNum);
        for (ResourceSummary resourceSummary : sServices.resources().querySummaries(sServices.toString(), rparams)) {
            if (!processResource(sServices, tServices, tInsp.getId(), resourceSummary)) {
                return false;
            }
        }
        
        return true;
    }
}
