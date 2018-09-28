package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSummary;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author Philip A. Chapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FeederDataDirProcessor.class);
    
    private static final String POLE_DATA_SUBDIR = "Pole_Photos_and_PF_Project_V2";
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment env, ImportProcessListener listener, File feederDir) {
        
        listener.setStatus(ImportProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        // There should be an excel file for all poles.
        boolean success = SurveyReportImport.processMasterSurveyTemplate(env, listener, data, feederDir);
        
        if (success) {
            File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
            if (poleDataDir.exists() && poleDataDir.canRead()) {
                listener.setStatus(ImportProcessStatus.ProcessingPoleData);
                for (File f : poleDataDir.listFiles()) {
                    if (f.isDirectory()) {
                        // The name of the directory should be the FPL ID of the pole.
                        Pole pole = data.getPoleDataByFPLId().get(f.getName());
                        if (pole == null) {
                            listener.reportMessage(String.format("No pole found with FPL ID \"%s\".  The directory \"%s\" is being skipped.", f.getName(), f.getAbsolutePath()));
                        } else {
                            success = PoleDataProcessor.process(env, listener, data, pole, f);
                            if (!success) {
                                break;
                            }
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
        
        if (success) {
            try {
                listener.setStatus(ImportProcessStatus.PersistingData);

                // Save SubStation
                if (data.getSubStation() != null) {
                    SubStationWebService sssvc = env.obtainWebService(SubStationWebService.class);
                    if (data.getDomainObjectIsNew().get(data.getSubStation().getId())) {
                        sssvc.create(env.obtainAccessToken(), data.getSubStation());
                    } else {
                        sssvc.update(env.obtainAccessToken(), data.getSubStation());
                    }
                }

                // Save Poles
                if (!data.getPoleDataByFPLId().isEmpty()) {
                    PoleWebService psvc = env.obtainWebService(PoleWebService.class);
                    for (Pole pdata : data.getPoleDataByFPLId().values()) {
                        if (data.getDomainObjectIsNew().get(pdata.getId())) {
                            psvc.create(env.obtainAccessToken(), pdata);
                        } else {
                            psvc.update(env.obtainAccessToken(), pdata);
                        }
                    }
                }

                // Save Pole Inspections
                if (!data.getPoleInspectionsByFPLId().isEmpty()) {
                    PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
                    for (PoleInspection pi : data.getPoleInspectionsByFPLId().values()) {
                        if (data.getDomainObjectIsNew().get(pi.getId())) {
                            pisvc.create(env.obtainAccessToken(), pi);
                        } else {
                            pisvc.update(env.obtainAccessToken(), pi);
                        }
                    }
                }
                
                // Now that we've saved all the data, we can update the survey report.
                updateSurveyReport(env, data, listener);
        
                listener.setStatus(ImportProcessStatus.UploadingResources);
                // Save Resources
                ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
                for (ResourceMetadata rmeta : data.getSubStationResources()) {
                    ResourceDataUploader.uploadResources(env, listener, data.getSubStationResources(), data.getResourceDataFiles(), 2);
                }
                for (List<ResourceMetadata> rmetaList : data.getPoleResources().values()) {
                    for (List<ResourceMetadata> list : data.getPoleResources().values()) {
                        ResourceDataUploader.uploadResources(env, listener, list, data.getResourceDataFiles(), 2);
                    }
                }
            } catch (Throwable t) {
                listener.reportFatalException("Error persisting inspection data.", t);
            }
        }
        
        listener.setStatus(ImportProcessStatus.Done);
        
        return success;
    }
    
    private static boolean updateSurveyReport(Environment env, InspectionData data, ImportProcessListener listener) {
        listener.setStatus(ImportProcessStatus.GeneratingUpdatedSurveyReport);
        try {
            File outFile = File.createTempFile(data.getSubStation().getFeederNumber(), "surveyrpt");
            SubStationSummary summary = env.obtainWebService(SubStationWebService.class).retrieveSummary(env.obtainAccessToken(), data.getSubStation().getId());
            boolean success = SurveyReportGenerator.populateTemplate(env, listener, summary, data.getMasterDataFile(), outFile);
            if (success) {
                ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
                ResourceSearchParameters params = new ResourceSearchParameters();
                params.setOrganizationId(data.getSubStation().getOrganizationId());
                params.setSubStationId(data.getSubStation().getId());
                params.setType(ResourceType.SurveyReport);
                ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(svc.query(env.obtainAccessToken(), params));
                if (rmeta == null) {
                    rmeta = new ResourceMetadata();
                    rmeta.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    rmeta.setName(String.format("%s-%s_Survey_Report.xlsx", data.getSubStation().getName(), data.getSubStation().getFeederNumber()));
                    rmeta.setOrganizationId(params.getOrganizationId());
                    rmeta.setResourceId(UUID.randomUUID().toString());
                    rmeta.setStatus(ResourceStatus.QueuedForUpload);
                    rmeta.setSubStationId(params.getSubStationId());
                    rmeta.setTimestamp(ZonedDateTime.now());
                    rmeta.setType(params.getType());
                    data.addResourceMetadata(rmeta, outFile, true);
                } else {
                    data.addResourceMetadata(rmeta, outFile, false);
                }
            }
        } catch (IOException ioe) {
            listener.reportNonFatalException("Error creating temporary file for updated Survey Report", ioe);
        }
        //TODO:
        return true;
    }
}
