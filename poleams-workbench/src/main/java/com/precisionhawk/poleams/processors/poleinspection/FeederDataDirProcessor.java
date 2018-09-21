package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.poledata.PoleData;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author pchapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FeederDataDirProcessor.class);
    
    private static final String POLE_DATA_SUBDIR = "Pole_Photos_and_PF_Project_V2";
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment environment, ProcessListener listener, File feederDir) {
        
        listener.setStatus(ProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        // There should be an excel file for all poles.
        boolean success = MasterSurveyTemplateProcessor.processMasterSurveyTemplate(environment, listener, data, feederDir);
        
        if (success) {
            File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
            if (poleDataDir.exists() && poleDataDir.canRead()) {
                for (File f : poleDataDir.listFiles()) {
                    if (f.isDirectory()) {
                        success = PoleDataProcessor.process(environment, listener, data, f);
                        if (!success) {
                            break;
                        }
                    } else {
                        listener.reportMessage(String.format("File \"%s\" is not a directory and is being skipped.", f));
                    }
                }
            } else {
                listener.reportFatalError(String.format("The parent directory for pole data \"%s\" does not exist or cannot be read.", poleDataDir));
                success = false;
            }            
        }
        
        try {
            if (data.getSubStation() != null) {
                SubStationWebService sssvc = environment.obtainWebService(SubStationWebService.class);
                if (data.getSubStation().getId() == null) {
                    sssvc.create(environment.obtainAccessToken(), data.getSubStation());
                } else {
                    sssvc.update(environment.obtainAccessToken(), data.getSubStation());
                }
            }

            if (!data.getPoleData().isEmpty()) {
                PoleWebService psvc = environment.obtainWebService(PoleWebService.class);
                for (PoleData pdata : data.getPoleData().values()) {
                    if (data.getPoleDataIsNew().get(pdata.getId())) {
                        psvc.create(environment.obtainAccessToken(), pdata);
                    } else {
                        psvc.update(environment.obtainAccessToken(), pdata);
                    }
                }
            }
            
            //TODO: Resources
        } catch (IOException ioe) {
            listener.reportFatalException("Error persisting inspection data.", ioe);
        }
        
        //TODO:
        return success;
    }
}
