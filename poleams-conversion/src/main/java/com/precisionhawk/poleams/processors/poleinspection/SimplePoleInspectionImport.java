package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.GeoPoint;
import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * A very simplistic pole inspection that ingests images for poles and nothing more.
 *
 * @author pchapman
 */
public final class SimplePoleInspectionImport extends AbstractInspectionImport {
    
    private static final TypeIdentifier TYPE_IDENTIFIER = new TypeIdentifier() {
        @Override
        public ResourceType identifyType(String name) {
            name = name.toLowerCase();
            if (name.contains("dji")) {
                return ResourceType.DroneInspectionImage;
            } else {
                return ResourceType.ManualInspectionImage;
            }
        }
    };
    private static final ImagesProcessor IMAGES_PROCESSOR;
    static {
        IMAGES_PROCESSOR = new ImagesProcessor();
        IMAGES_PROCESSOR.setIdentifier(TYPE_IDENTIFIER);
    }
    
    private SimplePoleInspectionImport() {}
    
    /**
     * Imports images for a simple pole inspection. Inside the directory provided,
     * the import expects to find other directories. One for each of the poles to
     * import.  The directories will each will contain images for the pole that has
     * been inspected.  All images that contain &quot;DJI&quot; in the name (not
     * case sensitive) will be considered arial images.  Any other images are
     * assumed to be ground-based images.  There should be an image that starts
     * with the number "1" which will indicate the image to use for obtaining the
     * pole's GPS coordinates.
     * @param env The environment to import the data into.
     * @param listener A listener to be notified of progress.
     * @param feederId The utility assigned feeder ID.
     * @param feederDir The directory in which the pole image directories are to be
     *                  found.
     * @return True if the import was successful, else false.
     */
    public static boolean process(Environment env, ImportProcessListener listener, String feederId, File feederDir) {
        boolean success = true;
        
        listener.setStatus(ImportProcessStatus.Initializing);
        InspectionData data = new InspectionData();
        
        success = lookupSite(env, listener, feederId, data);
        
        if (!success) {
            return success;
        }
        
        listener.setStatus(ImportProcessStatus.ProcessingPoleData);
        for (File poleDir : feederDir.listFiles()) {
            if (poleDir.isDirectory()) {
                success = processPoleDirectory(env, listener, poleDir, data);
                if (!success) {
                    return success;
                }
            }
        }

        try {
            listener.setStatus(ImportProcessStatus.PersistingData);

            savePoleData(env, listener, data);

            listener.setStatus(ImportProcessStatus.UploadingResources);
            
            saveAndUploadResources(env, listener, data);
            
            zoomifyImages(env, listener, data);
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
        
        listener.setStatus(ImportProcessStatus.Done);

        return success;
    }

    private static boolean processImageFile(Environment env, ImportProcessListener listener, Pole pole, File imageFile, InspectionData data) {
        try {
            ImageFormat format = Imaging.guessFormat(imageFile);
            if (ImageFormats.UNKNOWN.equals(format)) {
                listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", imageFile.getAbsolutePath()));
            } else {
                ResourceMetadata rmeta = IMAGES_PROCESSOR.process(env, listener, data, pole, imageFile, format);
                if (rmeta != null && imageFile.getName().startsWith("1_")) {
                    // Use this image to set GPS coordinates for pole
                    if (rmeta.getLocation() == null || rmeta.getLocation().getLatitude() == null || rmeta.getLocation().getLongitude() == null) {
                        listener.reportNonFatalError(String.format("The image \"%s\" does not contain GPS coordinates.  GPS coordinates not set for pole %s.", imageFile.getAbsoluteFile(), pole.getFPLId()));
                    } else {
                        pole.setLocation(new GeoPoint(rmeta.getLocation()));
                    }
                }
            }
            return true;
        } catch (ImageReadException | IOException ex) {
            listener.reportFatalException(String.format("Error processing the file \"%s\"", imageFile.getAbsolutePath()), ex);
            return false;
        }
    }

    private static boolean processPoleDirectory(Environment env, ImportProcessListener listener, File poleDir, InspectionData data) {
        try {
            String utilityId = poleDir.getName().replace("Pole ", "");
            PoleSearchParameters pparams = new PoleSearchParameters();
            pparams.setFPLId(utilityId);
            PoleInspection insp = null;
            Pole pole = CollectionsUtilities.firstItemIn(env.obtainWebService(PoleWebService.class).search(env.obtainAccessToken(), pparams));
            if (pole == null) {
                pole = new Pole();
                pole.setFPLId(utilityId);
                pole.setId(UUID.randomUUID().toString());
                pole.setOrganizationId("5042b09b-519d-4351-ad55-313fa085ec33"); //FIXME:
                pole.setSubStationId(data.getSubStation().getId());
                data.addPole(pole, true);
            } else {
                data.addPole(pole, false);
                // Search for inspection
                PoleInspectionSearchParameters piparams = new PoleInspectionSearchParameters();
                piparams.setPoleId(pole.getId());
                insp = CollectionsUtilities.firstItemIn(env.obtainWebService(PoleInspectionWebService.class).search(env.obtainAccessToken(), piparams));
            }
            if (insp == null) {
                insp = new PoleInspection();
                insp.setDateOfAnalysis(LocalDate.of(2018, 12, 4)); //FIXME: Hardcoded
                insp.setId(UUID.randomUUID().toString());
                insp.setOrganizationId("5042b09b-519d-4351-ad55-313fa085ec33"); //FIXME:
                insp.setPoleId(pole.getId());
                insp.setSubStationId(data.getSubStation().getId());
                data.addPoleInspection(pole, insp, true);
            } else {
                data.addPoleInspection(pole, insp, false);
            }
            for (File imageFile : poleDir.listFiles()) {
                if (imageFile.isFile()) {
                    if (!processImageFile(env, listener, pole, imageFile, data)) {
                        return false;
                    }
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException("Error obtaining access token.", ex);
            return false;
        }
        return true;
    }

    private static boolean lookupSite(Environment env, ImportProcessListener listener, String feederId, InspectionData data) {
        // We expect the substation to already exist
        try {
            SubStationSearchParameters params = new SubStationSearchParameters();
            params.setFeederNumber(feederId);
            data.setSubStation(
                    CollectionsUtilities.firstItemIn(
                            env.obtainWebService(SubStationWebService.class).search(env.obtainAccessToken(), params)
                    )
            );
        } catch (IOException ex) {
            listener.reportFatalException("Error obtaining access token.", ex);
        }
        if (data.getSubStation() == null) {
            return false;
        } else {
            data.getDomainObjectIsNew().put(data.getSubStation().getId(), false);
            return true;
        }
    }
    
    private static final String ZOOMIFY_URL = "http://processor.inspectools.net/poleams/zoomify";
    
    private static boolean zoomifyImages(Environment env, ImportProcessListener listener, InspectionData data) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost req = new HttpPost(ZOOMIFY_URL);
        req.getParams().setParameter("env", env.getName());
        boolean success = true;
        for (String utilityId : data.getPoleResources().keySet()) {
            success = success && zoomifyImages(env, listener, client, req, data.getPoleResources().get(utilityId));
        }
        success = success && zoomifyImages(env, listener, client, req, data.getSubStationResources());
        return success;
    }
    
    private static boolean zoomifyImages(Environment env, ImportProcessListener listener, CloseableHttpClient client, HttpPost req, Collection<ResourceMetadata> resources) throws IOException {
        for (ResourceMetadata rmeta : resources) {
            try {
                req.getParams().setParameter("resource", rmeta.getResourceId());
                req.setHeader("Authorized", "Bearer " + env.obtainAccessToken());
                client.execute(req);
                listener.reportMessage(String.format("The resource \"%s\" has been zoomified.", rmeta.getResourceId()));
            } finally {
                req.reset();
            }
        }
        return true;
    }
}
