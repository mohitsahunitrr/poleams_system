package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.processors.InspectionDataInterface;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ResourceDataUploader;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.type.TypeReference;
import org.papernapkin.liana.util.StringUtil;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "fixData";
    
    // Fix inspection dates for RockyMount as follows from Irene:
    /*
        Structures 1-16: 10-22-2018
        Structures 17-51: 10-23-2018
        Structures 52-88: 10-24-2018
        Structures 89-151: 10-25-2018
    */
    private final LocalDate[] dates = new LocalDate[] {
        LocalDate.of(2018, 10, 22),
        LocalDate.of(2018, 10, 23),
        LocalDate.of(2018, 10, 24),
        LocalDate.of(2018, 10, 25)
    };

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper services = new WSClientHelper(env);
        try {
            TransmissionStructureSearchParams tsparams = new TransmissionStructureSearchParams();
            tsparams.setSiteId("19d51cdb-c7b7-4b17-84f9-d26463775875");
            for (TransmissionStructure struct : services.transmissionStructures().search(services.token(), tsparams)) {
                fixInspectionDate(services, struct);
                fixResourceTimestamps(services, struct);
            }
            
//            FeederInspection insp = services.feederInspections().retrieve(services.token(), "35802c50-bef5-4da7-8737-388f61c52b91");
//            FeederInspectionSummary smry = services.feederInspections().retrieveSummary(services.token(), "35802c50-bef5-4da7-8737-388f61c52b91");
//            uploadReports(services, insp, smry);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }
    
    private void fixInspectionDate(WSClientHelper services, TransmissionStructure struct) throws IOException {
        TransmissionStructureInspection inspection;
        LocalDate inspectionDate;
        Integer i = Integer.valueOf(struct.getStructureNumber());
        inspectionDate = null;
        if (i < 1 || i > 151) {
            throw new IllegalArgumentException(String.format("Invalid structure number %d", i));
        } else if (i < 17) {
            // Structures 1-16: 10-22-2018
            inspectionDate = dates[0];
        } else if (i < 52) {
            // Structures 17-51: 10-23-2018
            inspectionDate = dates[1];
        } else if (i < 89) {
            // Structures 52-88: 10-24-2018
            inspectionDate = dates[2];
        } else if (i < 152) {
            // Structures 89-151: 10-25-2018
            inspectionDate = dates[3];
        }
        if (inspectionDate != null) {
            AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
            aiparams.setAssetId(struct.getId());
            inspection = CollectionsUtilities.firstItemIn(services.transmissionStructureInspections().search(services.token(), aiparams));
            if (inspection == null) {
                System.err.printf("No inspection found for structure number %s, id %s\n", struct.getStructureNumber(), struct.getId());
            } else {
                System.out.printf("Updating inspection date for structure number %s, id %s\n", struct.getSerialNumber(), struct.getId());
                inspection.setDateOfInspection(inspectionDate);
                services.transmissionStructureInspections().update(services.token(), inspection);
            }
        }
    }
    
    private final java.util.Comparator<ResourceMetadata> RM_COMP = new Comparator<ResourceMetadata>() {
        @Override
        public int compare(ResourceMetadata o1, ResourceMetadata o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    // Fix images so that they are listed in order by name.  To do this, we order the timestamps accordingly.
    private void fixResourceTimestamps(WSClientHelper services, TransmissionStructure struct) throws IOException {
        System.out.printf("Adjusting resource timestamps for structure number %s, id %s\n", struct.getSerialNumber(), struct.getId());
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetId(struct.getId());
        List<ResourceMetadata> resources = services.resources().search(services.token(), params);
        List<ZonedDateTime> times = new ArrayList<>(resources.size());
        ResourceMetadata rmeta;
        for (Iterator<ResourceMetadata> iter = resources.iterator(); iter.hasNext(); ) {
            rmeta = iter.next();
            if (rmeta.getContentType().startsWith("image/") && rmeta.getTimestamp() != null) {
                times.add(rmeta.getTimestamp());
            } else {
                iter.remove();
            }
        }
        Collections.sort(resources, RM_COMP);
        Collections.sort(times);
        for (int i = 0; i < resources.size(); i++) {
            rmeta = resources.get(i);
            rmeta.setTimestamp(times.get(i));
            services.resources().updateResourceMetadata(services.token(), rmeta);
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        // Do Nothing
    }

    private void uploadReports(WSClientHelper services, FeederInspection insp, FeederInspectionSummary smry)
        throws IOException
    {
        // Download summary from nobhill in "production"
        URL url = new URL("https://services.inspectools.net/poleams/subStation/78de1f0b-471c-4c43-a7be-1c98482bf6df/summary");
        Map<String, Object> data = ObjectMapperFactory.getObjectMapper().readValue(url.openStream(), new TypeReference<Map<String, Object>>(){});
        Object downloadURL;
        Collection<ResourceMetadata> metaData = new LinkedList<>();
        Map<String, File> resourceData = new HashMap<>();
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getAnomalyMapDownloadURL(), ResourceTypes.FeederAnomalyMap, "application/pdf", data, "anomalyMapDownloadURL");
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getAnomalyReportDownloadURL(), ResourceTypes.FeederAnomalyReport, "application/pdf", data, "anomalyReportDownloadURL");
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getFeederMapDownloadURL(), ResourceTypes.FeederMap, "application/pdf", data, "feederMapDownloadURL");
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getSummaryReportDownloadURL(), ResourceTypes.FeederSummaryReport, "application/pdf", data, "summaryReportDownloadURL");
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getVegitationEncroachmentReportDownloadURL(), ResourceTypes.EncroachmentReport, "application/pdf", data, "vegitationEncroachmentReportDownloadURL");
        populateResource(metaData, resourceData, insp, smry, null, null, smry.getVegitationEncroachmentShapeDownloadURL(), ResourceTypes.EncroachmentShape, "application/vnd.google-earth.kmz", data, "vegitationEncroachmentShapeDownloadURL");
        PoleInspectionSummary pismry;
        for (String utilityId : smry.getPoleInspectionsByFPLId().keySet()) {
            pismry = smry.getPoleInspectionsByFPLId().get(utilityId);
            populateResource(metaData, resourceData, insp, smry, pismry.getAssetId(), pismry.getId(), pismry.getAnalysisReportURL(), ResourceTypes.PoleInspectionReport, "application/pdf", data, "poleInspectionsByFPLId", utilityId, "analysisReportURL");
            populateResource(metaData, resourceData, insp, smry, pismry.getAssetId(), pismry.getId(), pismry.getAnalysisResultURL(), ResourceTypes.PoleInspectionAnalysisXML, "application/xml", data, "poleInspectionsByFPLId", utilityId, "analysisResultURL");
            populateResource(metaData, resourceData, insp, smry, pismry.getAssetId(), pismry.getId(), pismry.getDesignReportURL(), ResourceTypes.PoleDesignReport, "application/pdf", data, "poleInspectionsByFPLId", utilityId, "designReportURL");
        }
        if (insp.getVegitationEncroachmentGoogleEarthURL() == null) {
            insp.setVegitationEncroachmentGoogleEarthURL(StringUtil.nullableToString(data.get("vegitationEncroachmentGoogleEarthURL")));
            services.feederInspections().update(services.token(), insp);
        }
        if (metaData.isEmpty()) {
            System.out.println("No resources to upload.");
        } else {
            ProcessListener listener = new ProcessListener() {
                @Override
                public void reportFatalError(String message) {
                    System.err.println(message);
                }
                @Override
                public void reportFatalException(String message, Throwable t) {
                    System.err.println(message);
                    t.printStackTrace(System.err);
                }
                @Override
                public void reportFatalException(Exception ex) {
                    ex.printStackTrace(System.err);
                }
                @Override
                public void reportMessage(String message) {
                    System.out.println(message);
                }
                @Override
                public void reportNonFatalError(String message) {
                    System.err.println(message);
                }
                @Override
                public void reportNonFatalException(String message, Throwable t) {
                    System.err.println(message);
                    t.printStackTrace(System.err);
                }
            };
            final Map<String, Boolean> isNew = new HashMap<>();
            for (ResourceMetadata rmeta : metaData) {
                isNew.put(rmeta.getResourceId(), Boolean.TRUE);
            }
            InspectionDataInterface impl = new InspectionDataInterface() {
                @Override
                public Map<String, Boolean> getDomainObjectIsNew() {
                    return isNew;
                }  
            };
            ResourceDataUploader.uploadResources(services.getEnv(), listener, impl, metaData, resourceData, 3);
        }
    }

    private void populateResource(Collection<ResourceMetadata> metaData, Map<String, File> resourceData, FeederInspection insp, FeederInspectionSummary smry, String assetId, String assetInspectionId, String reportURL, ResourceType resourceType, String contentType, Map<String, Object> data, String ... dataPath) throws IOException {
        if (reportURL == null) {
            Map<String, Object> curObj = data;
            Object value = null;
            for (String elem : dataPath) {
                value = curObj.get(elem);
                if (value instanceof Map) {
                    curObj = (Map<String, Object>)value;
                    value = null;
                }
            }
            if (value == null) {
                System.err.printf("%s not found, unable to populate.\n", resourceType.toString());
            } else {
                // We have a value.  Download the report from old, upload it into new
                String ext = contentType.startsWith("image/") ? ".png" : "pdf";
                File outfile = File.createTempFile("", ext);
                URL source = new URL(value.toString());
                InputStream is = source.openStream();
                OutputStream os = new FileOutputStream(outfile);
                IOUtils.copy(is, os);
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
                ResourceMetadata rmeta = new ResourceMetadata();
                rmeta.setAssetId(assetId);
                rmeta.setAssetInspectionId(assetInspectionId);
                rmeta.setResourceId(UUID.randomUUID().toString());
                rmeta.setContentType(contentType);
                rmeta.setName(rmeta.getResourceId() + "." + ext);
                rmeta.setOrderNumber(insp.getOrderNumber());
                rmeta.setSiteId(insp.getSiteId());
                rmeta.setSiteInspectionId(insp.getId());
                rmeta.setStatus(ResourceStatus.QueuedForUpload);
                rmeta.setTimestamp(ZonedDateTime.now());
                rmeta.setType(resourceType);
                metaData.add(rmeta);
                resourceData.put(rmeta.getResourceId(), outfile);
            }
        } // else, no need to download and re-upload it.
    }
}
