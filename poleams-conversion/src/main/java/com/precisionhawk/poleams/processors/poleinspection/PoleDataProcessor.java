package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

/**
 *
 * @author Philip A. Chapman
 */
final class PoleDataProcessor {
    
    static boolean processImagesForPole(Environment env, ProcessListener listener, InspectionData data, Pole p, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormats.UNKNOWN.equals(format)) {
                            listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", f));
                        } else {
                            ImagesProcessor.process(env, listener, data, p, f, format);
                        }
                    } catch (ImageReadException | IOException ex) {
                        listener.reportNonFatalException(String.format("There was an error parsing resource file \"%s\"", f.getAbsolutePath()), ex);

                        return true;
                    }
                } else {
                    listener.reportNonFatalError(String.format("The file \"%s\" is not readable.", f));
                }
            } else {
                listener.reportMessage(String.format("The directory \"%s\" is being ignored.", f));
            }
        }
        return true;
    }
    
    static boolean processPoleForemanOutput(Environment env, ProcessListener listener, InspectionData data, File dir) {
        ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
        Pole p;
        ResourceSearchParameters params;
        ResourceMetadata rmeta;
        for (File f : dir.listFiles()) {
            p = poleForPoleForemanOutFile(env, listener, data, f);
            if (p != null) {
                try {
                    String fname = f.getName().toUpperCase();
                    params = new ResourceSearchParameters();
                    params.setPoleId(p.getId());
                    params.setPoleInspectionId(data.getPoleInspectionsByFPLId().get(p.getFPLId()).getId());
                    if (fname.endsWith("_250C.XML")) {
                        // We have the PoleForeman data file.
                        PoleForemanXMLProcessor.process(listener, p, data.getPoleInspectionsByFPLId().get(p.getFPLId()), f);
                        params = new ResourceSearchParameters();
                        params.setPoleId(p.getId());
                        params.setPoleInspectionId(data.getPoleInspectionsByFPLId().get(p.getFPLId()).getId());
                        params.setType(ResourceType.PoleInspectionAnalysisXML);
                        rmeta = CollectionsUtilities.firstItemIn(rsvc.query(env.obtainAccessToken(), params));
                        if (rmeta == null) {
                            rmeta = createMetadata(data, params, f, "application/xml");
                            data.addResourceMetadata(rmeta, f, true);
                        } else {
                            data.addResourceMetadata(rmeta, f, false);
                        }
                    } else if (fname.endsWith("_250C.PDF")) {
                        // Assume it's the Pole Foreman report
                        params.setType(ResourceType.PoleInspectionReport);
                        rmeta = CollectionsUtilities.firstItemIn(rsvc.query(env.obtainAccessToken(), params));
                        if (rmeta == null) {
                            rmeta = createMetadata(data, params, f, "application/pdf");
                            data.addResourceMetadata(rmeta, f, true);
                        } else {
                            data.addResourceMetadata(rmeta, f, false);
                        }
                    } else {
                        listener.reportMessage(String.format("Unrecognized file \"%s\" has been skipped.", f.getAbsolutePath()));
                    }
                } catch (IOException ex) {
                    listener.reportNonFatalException(String.format("There was an querying resource metadata for file \"%s\".  The file will be skipped.", f.getAbsolutePath()), ex);
                }
            }
        }
        
        return true;
    }
    
    private static Pole poleForPoleForemanOutFile(Environment env, ProcessListener listener, InspectionData data, File pfFile) {
        if (pfFile.isDirectory()) {
            listener.reportNonFatalError(String.format("Unexpected directory \"%s\" is being skipped.", pfFile));
            return null;
        } else if (!pfFile.canRead()) {
            listener.reportNonFatalError(String.format("Unable to read the file \"%s\".  It is being skipped.", pfFile));
            return null;
        } else {
            String fplid = pfFile.getName().split("_")[0];
            Pole p = data.getPoleDataByFPLId().get(fplid);
            if (p == null) {
                PoleSearchParameters params = new PoleSearchParameters();
                params.setFPLId(fplid);
                try {
                    p = CollectionsUtilities.firstItemIn(env.obtainWebService(PoleWebService.class).search(env.obtainAccessToken(), params));
                } catch (IOException ioe) {
                    listener.reportNonFatalException(String.format("Error looking up FPL ID \"%s\"", fplid), ioe);
                }
                if (p == null) {
                    listener.reportNonFatalError(String.format("No pole with FPL ID \"%s\" found. The file \"%s\" is being skipped.", fplid, pfFile));
                } else {
                    data.addPole(p, false);
                }
            }
            return p;
        }
    }
    
    private static ResourceMetadata createMetadata(InspectionData data, ResourceSearchParameters params, File f, String contentType) {
        ResourceMetadata rmeta = new ResourceMetadata();
        rmeta.setContentType(contentType);
        rmeta.setName(f.getName());
        rmeta.setOrganizationId(data.getSubStation().getOrganizationId());
        rmeta.setPoleId(params.getPoleId());
        rmeta.setPoleInspectionId(params.getPoleInspectionId());
        rmeta.setResourceId(UUID.randomUUID().toString());
        rmeta.setStatus(ResourceStatus.QueuedForUpload);
        rmeta.setSubStationId(data.getSubStation().getId());
        rmeta.setTimestamp(ZonedDateTime.now());
        rmeta.setType(params.getType());
        return rmeta;
    }
}
