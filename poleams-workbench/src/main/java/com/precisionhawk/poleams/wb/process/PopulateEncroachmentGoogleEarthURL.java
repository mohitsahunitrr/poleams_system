package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class PopulateEncroachmentGoogleEarthURL extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_URL = "-url";
    private static final String COMMAND = "populateEncroachmentURL";

    private static final String HELP =
            "\t" + COMMAND + " " + ARG_FEEDER_ID + " FeederId "
            + ARG_URL + " https://url";

    private String feederId;
    private String geURL;

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
        if (feederId == null || feederId.isEmpty() || geURL == null || geURL.isEmpty()) {
            return false;
        }
        SubStationWebService ssvc = env.obtainWebService(SubStationWebService.class);
        SubStationSearchParameters params = new SubStationSearchParameters();
        params.setFeederNumber(feederId);
        try {
            SubStation ss = CollectionsUtilities.firstItemIn(ssvc.search(env.obtainAccessToken(), params));
            if (ss == null) {
                System.err.printf("No Feeder found for feeder ID %s\n", feederId);
            } else {
                ss.setVegitationEncroachmentGoogleEarthURL(geURL);
                ssvc.update(env.obtainAccessToken(), ss);
                System.err.printf("Feeder %s saved with new URL\n", feederId);
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
