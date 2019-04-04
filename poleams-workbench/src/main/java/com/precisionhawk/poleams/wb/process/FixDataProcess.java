package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.ams.util.HttpClientUtil;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import com.precisionhawk.poleams.processors.poleinspection.duke.Constants;
import com.precisionhawk.poleams.support.geojson.GeoJsonFeature;
import com.precisionhawk.poleams.support.geojson.GeoJsonFeatureSet;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Queue;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.codehaus.jackson.map.ObjectMapper;
import org.papernapkin.liana.util.StringUtil;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String ARG_ORG = "-o";
    private static final String ARG_JSON = "-g";
    private static final String COMMAND = "fixData";
    
    private boolean dry = false;
    private String geoJson;
    private String org;
    private boolean updateStatus = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_DRY:
                if (dry) {
                    // Only pass the arg once
                    return false;
                } else {
                    dry = true;
                    return true;
                }
            case ARG_JSON:
                if (geoJson == null) {
                    geoJson = args.poll();
                    return geoJson != null;
                } else {
                    return false;
                }
            case ARG_ORG:
                if (org == null) {
                    org = args.poll();
                    return org != null;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (!StringUtil.notNullNotEmpty(org)) {
            System.err.println("Org ID required");
            return false;
        }
        updateStatus = Constants.DUKE_ORG_ID.equals(org);
        WSClientHelper svcs = new WSClientHelper(env);
        
        HashMap<String, GeoJsonFeature> featuresMap = null;
        if (geoJson != null) {
            System.out.printf("Parsing features from %s\n", geoJson);
            GeoJsonFeatureSet featureSet = null;
            // Load the data from the GeoJson
            ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
            try {
                featureSet = mapper.readValue(new File(geoJson), GeoJsonFeatureSet.class);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                return true;
            }
            
            featuresMap = new HashMap();
            String serialNumber;

            for (GeoJsonFeature feature : featureSet.getFeatures()) {
                serialNumber = feature.getProperties().get("POLE_NUMBE");
                if (serialNumber == null) {
                    System.err.printf("No pole number for pole %s\n", feature.getProperties().get("POLEID"));
                } else {
                    featuresMap.put(serialNumber, feature);
                }
            }
        }
        
        
        InspectionData data = new InspectionData();

        try {
            FeederSearchParams params = new FeederSearchParams();
            params.setOrganizationId(org);
            for (Feeder feeder : svcs.feeders().search(svcs.token(), params)) {
                processFeeder(svcs, data, featuresMap, feeder);
            }
            
            if (!dry) {
                ProcessListener listener = new CLIProcessListener();
                DataImportUtilities.saveData(svcs, listener, data);
                DataImportUtilities.saveResources(svcs, listener, data);
            }
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

    private void processFeeder(WSClientHelper svcs, InspectionData data, HashMap<String, GeoJsonFeature> featuresMap, Feeder feeder) throws IOException {
        System.out.printf("Processing feeder %s\n", feeder.getFeederNumber());
        PoleSearchParams params = new PoleSearchParams();
        params.setSiteId(feeder.getId());
        for (Pole pole : svcs.poles().search(svcs.token(), params)) {
            processPole(svcs, data, featuresMap, pole);
        }
    }
    
    private void processPole(WSClientHelper svcs, InspectionData data, HashMap<String, GeoJsonFeature> featuresMap, Pole pole) throws IOException {
        System.out.printf("Processing pole %s\n", pole.getSerialNumber());
        if (pole.getLocation() == null) {
            GeoJsonFeature f = featuresMap.get(pole.getSerialNumber());
            if (f != null && f.getGeometry() != null) {
                GeoPoint p = new GeoPoint();
                p.setLongitude(f.getGeometry().getCoordinates()[0]);
                p.setLatitude(f.getGeometry().getCoordinates()[1]);
                pole.setLocation(p);
                data.addPole(pole, false);
                System.out.printf("Location updated for pole %s\n", pole.getSerialNumber());
            }
        }
        
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setAssetId(pole.getId());
        for (PoleInspection insp : svcs.poleInspections().search(svcs.token(), params)) {
            if (insp.getSiteId() == null) {
                insp.setSiteId(pole.getId());
                data.getPoleInspectionsMap().put(new SiteAssetKey(insp.getSiteId(), insp.getId()), insp);
                data.getDomainObjectIsNew().put(insp.getId(), false);
            }
//            processPoleInspection(svcs, data, insp);
        }
    }

    private void processPoleInspection(WSClientHelper svcs, InspectionData data, PoleInspection insp) throws IOException {
//        boolean hasResources = false;
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetInspectionId(insp.getId());
        for (ResourceSummary smry : svcs.resources().querySummaries(svcs.token(), params)) {
//            hasResources = true;
            processResource(svcs, data, smry);
        }
//        if (updateStatus && hasResources && Constants.AI_PENDING.equals(insp.getStatus())) {
//            insp.setStatus(Constants.AI_PROCESSED);
//            if (!data.getDomainObjectIsNew().containsKey(insp.getId())) {
//                data.getPoleInspectionsMap().put(new SiteAssetKey(insp.getSiteId(), insp.getId()), insp);
//                data.getDomainObjectIsNew().put(insp.getId(), false);
//            }
//        }
    }
    
    private void processResource(WSClientHelper svcs, InspectionData data, ResourceSummary smry) throws IOException {
        if (ResourceTypes.ManualInspectionImage.equals(smry.getType())) {
            ImageUtilities.ImageType imgType = ImageUtilities.ImageType.fromContentType(smry.getContentType());
            File inFile = File.createTempFile("paimg", "." + imgType.name());
            OutputStream os = new FileOutputStream(inFile);
            HttpClientUtil.downloadResource(smry.getDownloadURL(), os);
            os.close();
            try {
                TiffImageMetadata meta = ImageUtilities.retrieveExif(inFile);
                if (meta != null) {
                    File outFile = ImageUtilities.rotateIfNecessary(meta, inFile, imgType);
                    if (!inFile.equals(outFile)) {
                        // it had to be rotated.  queue it for upload.
                        ResourceMetadata rmeta = svcs.resources().retrieve(svcs.token(), smry.getResourceId());
                        rmeta.setStatus(ResourceStatus.QueuedForUpload);
                        data.addResourceMetadata(rmeta, outFile, false);
                    }
                    inFile.delete();
                }
            } catch (ImageReadException ex) {
                throw new IOException(ex);
            }
        }
    }
}
