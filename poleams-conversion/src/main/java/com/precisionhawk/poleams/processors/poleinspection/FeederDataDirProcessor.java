package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ContentTypeUtilities;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;

/**
 * Given a directory processes the Excel spreadsheet and pole analysis XML files
 * for pole data.  This data along with related artifacts uploaded into PoleAMS.
 *
 * @author Philip A. Chapman
 */
public final class FeederDataDirProcessor implements Constants {
    
    private static final String FEEDER_MAP_DIR = "2. Feeder Map";
    private static final Pattern FEEDER_MAP_REGEX = Pattern.compile("\\w*_\\d{5}\\.pdf");    
    private static final FilenameFilter FEEDER_MAP_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return FEEDER_MAP_REGEX.matcher(name).matches();
        }
    };
    
    private static final FilenameFilter IMAGES_DIR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.startsWith("003.") && name.endsWith("_pictures");
        }
    };
    
    private static final String POLE_DATA_SUBDIR = "1. Pole Photos and PF Project";
    private static final Pattern POLE_FOREMAN_FILES_DIR_REGEX = Pattern.compile("001.*\\w*_\\d{5}_xml_pdf");    
    private static final FilenameFilter POLE_FOREMAN_FILES_DIR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.startsWith("001.") && name.endsWith("_xml_pdf");
        }
    };
    
    private static final String MASTER_SURVEY_TEMPL_DIR = "5. MASTER Survey Template";

    //TODO: This code could probably be used elsewhere.  Refactor this and other places where we are searching for/creating resources
    private static boolean ensureResource(Environment env, InspectionData data, ProcessListener listener, FeederInspection feederInspection, PoleInspection poleInspection, ResourceType resourceType, File f) {
        try {
            ResourceSearchParams params = new ResourceSearchParams();
            params.setSiteId(feederInspection.getSiteId());
            if (poleInspection != null) {
                params.setAssetInspectionId(poleInspection.getId());
            }
            params.setType(resourceType);
            ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(env.obtainWebService(ResourceWebService.class).search(env.obtainAccessToken(), params));
            boolean isnew = false;
            if (rmeta == null) {
                String contentType = ContentTypeUtilities.guessContentType(f);
                if (contentType == null) {
                    listener.reportNonFatalError(String.format("Unable to determine content type for %s", f));
                    return false;
                }
                rmeta = new ResourceMetadata();
                rmeta.setContentType(contentType);
                rmeta.setName(f.getName());
                rmeta.setOrderNumber(data.getCurrentOrderNumber());
                if (poleInspection != null) {
                    rmeta.setAssetId(poleInspection.getAssetId());
                    rmeta.setAssetInspectionId(poleInspection.getId());
                }
                rmeta.setResourceId(UUID.randomUUID().toString());
                rmeta.setStatus(ResourceStatus.QueuedForUpload);
                rmeta.setSiteId(feederInspection.getSiteId());
                rmeta.setSiteInspectionId(feederInspection.getId());
                rmeta.setTimestamp(ZonedDateTime.now());
                rmeta.setType(resourceType);
                isnew = true;
            }
            data.addResourceMetadata(rmeta, f, isnew);
            return true;
        } catch (IOException ex) {
            listener.reportNonFatalException("Error querying for existing resources.", ex);
            return false;
        }
    }
    
    // no state
    private FeederDataDirProcessor() {};

    public static boolean process(Environment env, ProcessListener listener, File feederDir, String orgId, String orderNumber) {
        
        boolean success;
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNumber);
        data.setOrganizationId(orgId);
        
        // There should be an excel file for all poles.
        File mstDir = new File(feederDir, MASTER_SURVEY_TEMPL_DIR);
        if (mstDir.isDirectory()) {
            success = SurveyReportImport.processMasterSurveyTemplate(env, listener, data, mstDir);
            if (!success) {
                return success;
            }
        } else {
            listener.reportFatalError(String.format("Master survey template directory \"%s\" does not exist, is not readable, or is not a directory.", mstDir));
            return false;
        }
        
        File poleDataDir = new File(feederDir, POLE_DATA_SUBDIR);
        if (poleDataDir.exists() && poleDataDir.canRead()) {
        
            // Process pole foreman output
            File pfDir = findPoleForemanFilesDir(listener, poleDataDir);
            if (pfDir != null) {
                success = PoleDataProcessor.processPoleForemanOutput(env, listener, data, pfDir);
            }
            if (!success) {
                return success;
            }
            
            // Process pole directories which contain images.
            File imagesDir = findImagesDir(listener, poleDataDir);
            if (imagesDir != null) {
                String fplid;
                for (File f : imagesDir.listFiles()) {
                    if (f.isDirectory()) {
                        fplid = f.getName();
                        // Skip pole foreman files dir
                        if (fplid.endsWith("f")) {
                            fplid = fplid.substring(0, fplid.length() -1);
                        }
                        // The name of the directory should be the FPL ID of the pole.
                        Pole pole = data.getPolesMap().get(fplid);
                        if (pole == null) {
                            listener.reportMessage(String.format("No pole found with FPL ID \"%s\".  The directory \"%s\" is being skipped.", fplid, f.getAbsolutePath()));
                        } else {
                            listener.reportMessage(String.format("Processing images for pole %s", fplid));
                            success = PoleDataProcessor.processImagesForPole(env, listener, data, pole, f);
                            if (!success) {
                                break;
                            }
                        }
                    } else {
                        listener.reportMessage(String.format("File \"%s\" is not a directory and is being skipped.", f));
                    }
                }
            }
        } else {
            listener.reportFatalError(String.format("The parent directory for pole data \"%s\" does not exist or cannot be read.", poleDataDir));
            success = false;
        }            
        if (!success) {
            return success;
        }
        
        // Find feeder map
        File fmDir = new File(feederDir, FEEDER_MAP_DIR);
        if (fmDir.isDirectory()) {
            File f = findFeederMapFile(listener, fmDir);
            if (f != null) {
                success = ensureResource(env, data, listener, data.getCurrentFeederInspection(), null, ResourceTypes.FeederMap, f);
            }
            if (!success) {
                return false;
            }
        } else {
            listener.reportFatalError(String.format("Feeder map directory \"%s\" does not exist, is not readable, or is not a directory.", fmDir));
            return false;
        }
        
        //TODO: Find and upload kml (kmz)
        
        try {
            WSClientHelper wsclient = new WSClientHelper(env);
            
            if (!DataImportUtilities.saveData(wsclient, listener, data)) {
                return false;
            }

            // Now that we've saved all the data, we can update the survey report.
            updateSurveyReport(env, data, listener);

            // Save Resources
            listener.reportMessage("Uploading Feeder resources.");
            if (!DataImportUtilities.saveResources(wsclient, listener, data)) {
                return false;
            }            
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
        
        return success;
    }

    private static File findFeederMapFile(ProcessListener listener, File feederMapDir) {
        File[] files = feederMapDir.listFiles(FEEDER_MAP_FILE_FILTER);
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple files exist in directory \"%s\" which could be the feeder map.", feederMapDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("No file found in directory \"%s\" which could e identified as the feeder map.", feederMapDir));
            return null;
        } else {
            return files[0];
        }
    }

    private static File findImagesDir(ProcessListener listener, File poleDataDir) {
        File[] files = poleDataDir.listFiles(IMAGES_DIR_FILTER);
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple directories exist in directory \"%s\" which could contain images.", poleDataDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("No directory found in directory \"%s\" which contain images.", poleDataDir));
            return null;
        } else {
            return files[0];
        }
    }

    private static File findPoleForemanFilesDir(ProcessListener listener, File poleDataDir) {
        File[] files = poleDataDir.listFiles(POLE_FOREMAN_FILES_DIR_FILTER);
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple directories exist in directory \"%s\" which could contain pole forman files.", poleDataDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("No directory found in directory \"%s\" which contains pole foreman files.", poleDataDir));
            return null;
        } else {
            return files[0];
        }
    }
    
    private static boolean updateSurveyReport(Environment env, InspectionData data, ProcessListener listener) {
        try {
            File inFile = File.createTempFile(data.getCurrentFeeder().getFeederNumber(), "surveyrptsource");
            File outFile = File.createTempFile(data.getCurrentFeeder().getFeederNumber(), "surveyrptout");
            // Copy from source to temp file.  I do this because I don't trust POI to not overwrite the original.  I've had it do strange things.
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(data.getMasterDataFile());
                os = new FileOutputStream(inFile);
                IOUtils.copy(is, os);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
            // Populate summary into 2nd temp file
            FeederInspectionSummary summary = env.obtainWebService(FeederInspectionWebService.class).retrieveSummary(env.obtainAccessToken(), data.getCurrentFeederInspection().getId());
            boolean success = SurveyReportGenerator.populateTemplate(env, listener, summary, inFile, outFile, false);
            // Set up to upload temp file
            if (success) {
                ResourceWebService svc = env.obtainWebService(ResourceWebService.class);
                ResourceSearchParams params = new ResourceSearchParams();
                params.setOrderNumber(data.getCurrentOrderNumber());
                params.setSiteId(data.getCurrentFeederInspection().getSiteId());
                params.setSiteInspectionId(data.getCurrentFeederInspection().getId());
                params.setType(ResourceTypes.SurveyReport);
                ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(svc.search(env.obtainAccessToken(), params));
                if (rmeta == null) {
                    rmeta = new ResourceMetadata();
                    rmeta.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    rmeta.setName(String.format("%s-%s_Survey_Report.xlsx", data.getCurrentFeeder().getName(), data.getCurrentFeeder().getFeederNumber()));
                    rmeta.setOrderNumber(params.getOrderNumber());
                    rmeta.setResourceId(UUID.randomUUID().toString());
                    rmeta.setStatus(ResourceStatus.QueuedForUpload);
                    rmeta.setSiteId(params.getSiteId());
                    rmeta.setSiteInspectionId(params.getSiteInspectionId());
                    rmeta.setTimestamp(ZonedDateTime.now());
                    rmeta.setType(params.getType());
                    data.addResourceMetadata(rmeta, outFile, true);
                } else {
                    rmeta.setStatus(ResourceStatus.QueuedForUpload);
                    data.addResourceMetadata(rmeta, outFile, false);
                }
            }
        } catch (IOException ioe) {
            listener.reportNonFatalException("Error creating temporary file for updated Survey Report", ioe);
        }

        return true;
    }
}
