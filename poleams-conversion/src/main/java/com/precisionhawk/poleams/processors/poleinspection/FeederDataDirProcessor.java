package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSummary;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author Philip A. Chapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    // The directory should have one Excel spreadsheet (xlsx) that contains
    // manually gathered data about poles.  We process this first.  It should
    // Also contain a directory that follows the pattern feedername_feedernum_xml_pdf
    // such as "nobhill_70662_xml_pdf".  This folder will contain output from pole
    // foreman.  Specifically, it should contain the XML output as well as a report
    // in PDF format for each pole.  The names should follow the format
    // fplid_250C.pdf and fplid_250C.xml.  For example, 3652617_250C.pdf and
    // 3652617_250C.xml respectively.  There will also be a folder for each pole
    // which contains images associated with that pole.  Each of these folders will
    // have a name that is either the FPL ID or the FPL ID followed by a "f" such as
    // "3798819" or "3798819f".
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FeederDataDirProcessor.class);
    
    private static final String POLE_DATA_SUBDIR = "Pole_Photos_and_PF_Project_V2";
    
    // nobhill_70662_xml_pdf
    private static final Pattern POLE_FOREMAN_FILES_DIR_REGEX = Pattern.compile("\\w*_\\d{5}_xml_pdf");    
    private static final FilenameFilter POLE_FOREMAN_FILES_DIR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return POLE_FOREMAN_FILES_DIR_REGEX.matcher(name).matches();
        }
    };
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment env, ImportProcessListener listener, File feederDir) {
        
        listener.setStatus(ImportProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        // There should be an excel file for all poles.
        boolean success = SurveyReportImport.processMasterSurveyTemplate(env, listener, data, feederDir);
        if (!success) {
            return success;
        }
        
        File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
        if (poleDataDir.exists() && poleDataDir.canRead()) {
            listener.setStatus(ImportProcessStatus.ProcessingPoleData);
        
            // Process pole foreman output
            File pfDir = findPoleForemanFilesDir(listener, poleDataDir);
            if (pfDir != null) {
                success = PoleDataProcessor.processPoleForemanOutput(env, listener, data, pfDir);
            }
            if (!success) {
                return success;
            }
            
            // Process pole directories which contain images.
            String fplid;
            for (File f : poleDataDir.listFiles()) {
                if (f.isDirectory()) {
                    fplid = f.getName();
                    // Skip pole foreman files dir
                    if (!POLE_FOREMAN_FILES_DIR_REGEX.matcher(fplid).matches()) {
                        if (fplid.endsWith("f")) {
                            fplid = fplid.substring(0, fplid.length() -1);
                        }
                        // The name of the directory should be the FPL ID of the pole.
                        Pole pole = data.getPoleDataByFPLId().get(fplid);
                        if (pole == null) {
                            listener.reportMessage(String.format("No pole found with FPL ID \"%s\".  The directory \"%s\" is being skipped.", fplid, f.getAbsolutePath()));
                        } else {
                            success = PoleDataProcessor.processImagesForPole(env, listener, data, pole, f);
                            if (!success) {
                                break;
                            }
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
        if (!success) {
            return success;
        }
        
        try {
            listener.setStatus(ImportProcessStatus.PersistingData);

            // Save SubStation
            if (data.getSubStation() != null) {
                SubStationWebService sssvc = env.obtainWebService(SubStationWebService.class);
                if (data.getDomainObjectIsNew().get(data.getSubStation().getId())) {
                    sssvc.create(env.obtainAccessToken(), data.getSubStation());
                    listener.reportMessage(String.format("Inserted new sub station %s", data.getSubStation().getFeederNumber()));
                } else {
                    sssvc.update(env.obtainAccessToken(), data.getSubStation());
                    listener.reportMessage(String.format("Updating sub station %s", data.getSubStation().getFeederNumber()));
                }
            }

            // Save Poles
            if (!data.getPoleDataByFPLId().isEmpty()) {
                PoleWebService psvc = env.obtainWebService(PoleWebService.class);
                for (Pole pdata : data.getPoleDataByFPLId().values()) {
                    if (data.getDomainObjectIsNew().get(pdata.getId())) {
                        psvc.create(env.obtainAccessToken(), pdata);
                        listener.reportMessage(String.format("Inserted new pole %s FPL ID %s", pdata.getId(), pdata.getFPLId()));
                    } else {
                        psvc.update(env.obtainAccessToken(), pdata);
                        listener.reportMessage(String.format("Updated pole %s FPL ID %s", pdata.getId(), pdata.getFPLId()));
                    }
                }
            }

            // Save Pole Inspections
            if (!data.getPoleInspectionsByFPLId().isEmpty()) {
                PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
                for (PoleInspection pi : data.getPoleInspectionsByFPLId().values()) {
                    if (data.getDomainObjectIsNew().get(pi.getId())) {
                        pisvc.create(env.obtainAccessToken(), pi);
                        listener.reportMessage(String.format("Inserted new inspection for pole %s", pi.getPoleId()));
                    } else {
                        pisvc.update(env.obtainAccessToken(), pi);
                        listener.reportMessage(String.format("updated inspection for pole %s", pi.getPoleId()));
                    }
                }
            }

            // Now that we've saved all the data, we can update the survey report.
            updateSurveyReport(env, data, listener);

            listener.setStatus(ImportProcessStatus.UploadingResources);
            // Save Resources
            listener.reportMessage("Uploading SubStation resources.");
            ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
            for (ResourceMetadata rmeta : data.getSubStationResources()) {
                ResourceDataUploader.uploadResources(env, listener, data, data.getSubStationResources(), data.getResourceDataFiles(), 2);
            }
            int index = 1;
            for (List<ResourceMetadata> list : data.getPoleResources().values()) {
                listener.reportMessage(String.format("Uploading Pole resources ( %d of %d poles).", index++, data.getPoleResources().size()));
                ResourceDataUploader.uploadResources(env, listener, data, list, data.getResourceDataFiles(), 2);
            }
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
        
        listener.setStatus(ImportProcessStatus.Done);
        
        return success;
    }

    private static File findPoleForemanFilesDir(ImportProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(POLE_FOREMAN_FILES_DIR_FILTER);
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple directories exist in directory \"%s\" which could contain pole forman files.", feederDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("No directory found in directory \"%s\" which contains pole foreman files.", feederDir));
            return null;
        } else {
            return files[0];
        }
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
