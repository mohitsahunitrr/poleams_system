package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class DeleteDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String COMMAND = "deleteFeederData";

    private String feederId;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (feederId == null && ARG_FEEDER_ID.equals(arg)) {
            feederId = args.poll();
            return feederId != null;
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        if (feederId == null) {
            return false;
        }
        try {
            SubStationSearchParameters params = new SubStationSearchParameters();
            params.setFeederNumber(feederId);
            SubStation ss = CollectionsUtilities.firstItemIn(env.obtainWebService(SubStationWebService.class).search(env.obtainAccessToken(), params));
            if (ss == null) {
                System.out.printf("The feeder %s does not exist.\n", feederId);
            } else {
                deleteResources(env, ss);
                deletePoleInspections(env, ss);
                deletePoles(env, ss);
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
    public void printHelp(PrintStream output) {
        //TODO:
    }

    private void deleteResources(Environment env, SubStation ss) throws IOException {
        System.out.println("Deleting resources...");
        ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
        ResourceSearchParameters params = new ResourceSearchParameters();
        params.setSubStationId(ss.getId());
        for (ResourceMetadata rmeta : svc.query(env.obtainAccessToken(), params)) {
            svc.delete(env.obtainAccessToken(), rmeta.getResourceId());
        }
    }

    private void deletePoleInspections(Environment env, SubStation ss) throws IOException {
        System.out.println("Deleting pole inspections...");
        PoleInspectionWebService svc = env.obtainWebService(PoleInspectionWebService.class);
        PoleInspectionSearchParameters params = new PoleInspectionSearchParameters();
        params.setSubStationId(ss.getId());
        for (PoleInspection insp : svc.search(env.obtainAccessToken(), params)) {
            svc.delete(env.obtainAccessToken(), insp.getId());
        }
    }

    private void deletePoles(Environment env, SubStation ss) throws IOException {
        System.out.println("Deleting poles...");
        PoleWebService svc = env.obtainWebService(PoleWebService.class);
        PoleSearchParameters params = new PoleSearchParameters();
        params.setSubStationId(ss.getId());
        for (Pole p : svc.search(env.obtainAccessToken(), params)) {
            svc.delete(env.obtainAccessToken(), p.getId());
        }
    }
    
}
