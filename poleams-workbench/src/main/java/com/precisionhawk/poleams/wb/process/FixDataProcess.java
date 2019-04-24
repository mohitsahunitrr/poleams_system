package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.Dimension;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
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
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;
import java.util.UUID;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
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
    private boolean isDuke = false;

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
        isDuke = Constants.DUKE_ORG_ID.equals(org);
        WSClientHelper svcs = new WSClientHelper(env);
        
        InspectionData data = new InspectionData();

        try {
            FeederSearchParams params = new FeederSearchParams();
            params.setOrganizationId(org);
            for (Feeder feeder : svcs.feeders().search(svcs.token(), params)) {
                processFeeder(svcs, data, feeder);
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

    private void processFeeder(WSClientHelper svcs, InspectionData data, Feeder feeder) throws IOException {
        System.out.printf("Processing feeder %s\n", feeder.getFeederNumber());
        PoleSearchParams params = new PoleSearchParams();
        params.setSiteId(feeder.getId());
        for (Pole pole : svcs.poles().search(svcs.token(), params)) {
            processPole(svcs, data, pole);
        }
    }
    
    private void processPole(WSClientHelper svcs, InspectionData data, Pole pole) throws IOException {
        System.out.printf("Processing pole %s\n", pole.getSerialNumber());
        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
        params.setAssetId(pole.getId());
        for (PoleInspection insp : svcs.poleInspections().search(svcs.token(), params)) {
            if (insp.getSiteId() == null) {
                insp.setSiteId(pole.getId());
                data.getPoleInspectionsMap().put(new SiteAssetKey(insp.getSiteId(), insp.getId()), insp);
                data.getDomainObjectIsNew().put(insp.getId(), false);
            }
            processPoleInspection(svcs, data, insp);
        }
    }

    private void processPoleInspection(WSClientHelper svcs, InspectionData data, PoleInspection insp) throws IOException {
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetInspectionId(insp.getId());
        for (ResourceSummary smry : svcs.resources().querySummaries(svcs.token(), params)) {
            processResource(svcs, data, smry);
        }
    }
    
    private void processResource(WSClientHelper svcs, InspectionData data, ResourceSummary smry) throws IOException {
        if (ResourceTypes.ManualInspectionImage.equals(smry.getType())) {
            ImageUtilities.ImageType imgType = ImageUtilities.ImageType.fromContentType(smry.getContentType());
            File inFile = File.createTempFile("paimg", "." + imgType.name());
            OutputStream os = new FileOutputStream(inFile);
            HttpClientUtil.downloadResource(smry.getDownloadURL(), os);
            os.close();
            try {
                ResourceMetadata rmeta = svcs.resources().retrieve(svcs.token(), smry.getResourceId());
                TiffImageMetadata meta = ImageUtilities.retrieveExif(inFile);
                if (isDuke) {
                    rmeta.setType(ResourceTypes.ManualInspectionImageZ);
                }
                if (meta != null) {
                    File outFile = ImageUtilities.rotateIfNecessary(meta, inFile, imgType);
                    if (!inFile.equals(outFile)) {
                        // It had to be rotated.  queue it for upload.  The original will be kept, but not shown.
                        ResourceMetadata rotated = new ResourceMetadata();
                        rotated.setAssetId(rmeta.getAssetId());
                        rotated.setAssetInspectionId(rmeta.getAssetInspectionId());
                        rotated.setComponentId(rmeta.getComponentId());
                        rotated.setComponentInspectionId(rmeta.getComponentInspectionId());
                        rotated.setContentType(rmeta.getContentType());
                        rotated.setLocation(rmeta.getLocation());
                        rotated.setName(rmeta.getName());
                        rotated.setOrderNumber(rmeta.getOrderNumber());
                        rotated.setPosition(rmeta.getPosition());
                        rotated.setResourceId(UUID.randomUUID().toString());
                        rotated.setSiteId(rmeta.getSiteId());
                        rotated.setSiteInspectionId(rmeta.getSiteInspectionId());
                        if (rmeta.getSize() != null) {
                            rotated.setSize(new Dimension(rmeta.getSize().getHeight(), rmeta.getSize().getWidth(), rmeta.getSize().getDepth()));
                        }
                        rotated.setSourceResourceId(rmeta.getResourceId());
                        rotated.setStatus(ResourceStatus.QueuedForUpload);
                        rotated.setTimestamp(rmeta.getTimestamp());
                        rotated.setType(rmeta.getType());
                        data.addResourceMetadata(rotated, outFile, false);
                        rmeta.setStatus(ResourceStatus.NotForDisplay);
                        data.addResourceMetadata(rmeta, null, false);
                    } else if (isDuke) {
                        rmeta.setStatus(ResourceStatus.Processed);
                        data.addResourceMetadata(rmeta, null, false);
                    }
                    inFile.delete();
                }
            } catch (ImageReadException ex) {
                throw new IOException(ex);
            }
        }
    }
}
