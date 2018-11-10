package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.FeederSummary;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.ResourceTypes;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import com.precisionhawk.poleams.webservices.FeederWebService;

/**
 *
 * @author pchapman
 */
public class UploadDesignReportsProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_SAMPLE = "-s";
    private static final String COMMAND = "designReports";
    
    private String sampleFilePath;
    private Map<String, String> designReportPaths = new HashMap<String, String>();

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_SAMPLE.equals(arg)) {
            if (sampleFilePath == null) {
                sampleFilePath = args.poll();
                return sampleFilePath != null;
            } else {
                return false;
            }
        } else {
            // Assume it's a FPL ID followed by the path to a file
            String fplid = arg;
            String path = args.poll();
            if (path != null) {
                designReportPaths.put(fplid, path);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (sampleFilePath == null || sampleFilePath.isEmpty()) {
            return false;
        }
        FeederWebService sssvc = env.obtainWebService(FeederWebService.class);
        PoleWebService psvc = env.obtainWebService(PoleWebService.class);
        PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
        ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
        
        try {
            boolean success = true;
            for (Feeder ss : sssvc.retrieveAll(env.obtainAccessToken())) {
                success = success && processSubStation(env, sssvc, psvc, pisvc, rsvc, ss);
                if (!success) {
                    break;
                }
            }
        } catch (IOException | URISyntaxException ex) {
            System.err.println("Error querying for substations");
            ex.printStackTrace(System.err);
        }
        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        // Do nothing
    }

    private boolean processSubStation(
        Environment env, FeederWebService sssvc, PoleWebService psvc, PoleInspectionWebService pisvc,
        ResourceWebService rsvc, Feeder ss
    )
        throws IOException, URISyntaxException
    {
        PoleInspectionSummary pis;
        FeederSummary sss = sssvc.retrieveSummary(env.obtainAccessToken(), ss.getId());
        for (String fplid : sss.getPoleInspectionsByFPLId().keySet()) {
            pis = sss.getPoleInspectionsByFPLId().get(fplid);
            if (designReportPaths.containsKey(fplid)) {
                uploadDesignReport(env, rsvc, fplid, pis, designReportPaths.get(fplid));
            } else {
                uploadDesignReport(env, rsvc, fplid, pis, sampleFilePath);
            }
        }
        
        return true;
    }

    private void uploadDesignReport(Environment env, ResourceWebService rsvc, String fplId, PoleInspectionSummary smry, String reportFilePath)
        throws IOException, URISyntaxException
    {
        File f = new File(reportFilePath);
        // First, see if the rmeta exists
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetInspectionId(smry.getId());
        params.setType(ResourceTypes.PoleDesignReport);
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.query(env.obtainAccessToken(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            rmeta.setContentType("application/pdf");
            rmeta.setName(f.getName());
            rmeta.setOrganizationId(smry.getOrganizationId());
            rmeta.setAssetId(smry.getPoleId());
            rmeta.setAssetInspectionId(smry.getId());
            rmeta.setResourceId(UUID.randomUUID().toString());
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(smry.getSubStationId());
            rmeta.setTimestamp(ZonedDateTime.now());
            rmeta.setType(ResourceTypes.PoleDesignReport);
            rsvc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
        } else {
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
        }
        System.out.printf("Uploading file \"%s\" for FPL ID %s\n", f, fplId);
        HttpClientUtilities.postFile(env, rmeta.getResourceId(), rmeta.getContentType(), f);
        rmeta.setStatus(ResourceStatus.Released);
        rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
    }
    
}
