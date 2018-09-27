package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
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
    
    static boolean process(Environment env, ProcessListener listener, InspectionData data, Pole p, File dir) {
        ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
        ResourceSearchParameters params;
        ResourceMetadata rmeta;
        
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormats.UNKNOWN.equals(format)) {
                            String fname = f.getName().toUpperCase();
                            params = new ResourceSearchParameters();
                            params.setPoleId(p.getId());
                            params.setPoleInspectionId(data.getPoleInspectionsByFPLId().get(p.getFPLId()).getId());
                            if (fname.endsWith("_250C.XML")) {
                                // We have the PoleForeman data file.
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
                            } else if (fname.endsWith(".PDF")) {
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
