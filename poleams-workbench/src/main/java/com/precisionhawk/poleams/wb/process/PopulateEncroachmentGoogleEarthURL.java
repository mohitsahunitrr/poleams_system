package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;
import com.precisionhawk.poleams.webservices.FeederWebService;

/**
 *
 * @author pchapman
 */
public class PopulateEncroachmentGoogleEarthURL extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_URL = "-url";
    private static final String COMMAND = "populateEncroachmentURL";

    private static final String HELP =
            "\t" + COMMAND + " " + ARG_FEEDER_ID + " FeederId "
            + ARG_ORDER_NUM + " WorkOrderNumber "
            + ARG_URL + " https://url";

    private String feederId;
    private String geURL;
    private String orderNumber;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_FEEDER_ID:
                if (feederId == null) {
                    feederId = args.poll();
                    return feederId != null;
                } else {
                    return false;
                }
            case ARG_ORDER_NUM:
                if (orderNumber == null) {
                    orderNumber = args.poll();
                    return orderNumber != null;
                } else {
                    return false;
                }
            case ARG_URL:
                if (geURL == null) {
                    geURL = args.poll();
                    return geURL != null;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (feederId == null || feederId.isEmpty() || geURL == null || geURL.isEmpty() || orderNumber == null || orderNumber.isEmpty()) {
            return false;
        }
        FeederInspectionWebService sisvc = env.obtainWebService(FeederInspectionWebService.class);
        FeederWebService ssvc = env.obtainWebService(FeederWebService.class);
        FeederSearchParams params = new FeederSearchParams();
        params.setFeederNumber(feederId);
        try {
            // Find Feeder
            Feeder ss = CollectionsUtilities.firstItemIn(ssvc.search(env.obtainAccessToken(), params));
            if (ss == null) {
                System.err.printf("No Feeder found for feeder ID %s\n", feederId);
            } else {
                // Find Feeder Inspection
                SiteInspectionSearchParams aiparams = new SiteInspectionSearchParams();
                aiparams.setSiteId(ss.getId());
                aiparams.setOrderNumber(orderNumber);
                FeederInspection fi = CollectionsUtilities.firstItemIn(sisvc.search(env.obtainAccessToken(), aiparams));
                if (fi == null) {
                    System.err.printf("Unable to locate feeder inspection for feeder %s, work order number %s\n", feederId, orderNumber);
                } else {
                    fi.setVegitationEncroachmentGoogleEarthURL(geURL);
                    sisvc.update(env.obtainAccessToken(), fi);
                    System.err.printf("Feeder Inspeciton for feeder %s, work order number %s saved with new URL\n", feederId, orderNumber);
                }
            }
        } catch (IOException ioe) {
            System.err.println("Error calling web services.");
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
    
}
