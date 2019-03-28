package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.FileFilters;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

/**
 * Imports Duke inspection data.
 * 
 * s3://ph-duke/organized/PriorityX/{site}/{pole_serial}/*.JPG
 *                  ^
 *             Entry Point
 * 
 * If camera make = DJI, we have a drone image, else ground.
 *
 * @author pchapman
 */
//TODO: Read directly from S3
public class InspectionImport {
    //FIXME: We need a better way
    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/New_York");
    private static final String DRONE_CAMERA_MAKE = "DJI";
    private static final String DUKE_ORG_ID = "c382f193-b687-432b-b838-3049b809c937";
    
    private InspectionImport() {}
    
    public static void process(Environment env, ProcessListener listener, File dir, String orderNumber)
    {
        Feeder feeder;
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNumber);

        try {
            WorkOrder wo = svcs.workOrders().retrieveById(svcs.token(), orderNumber);
            data.addWorkOrder(wo, false);
            data.setCurrentWorkOrder(wo);
        } catch (IOException ex) {
            listener.reportFatalException(String.format("Unable to lookup work order %s", orderNumber), ex);
        } catch (NotFoundException ex) {
            WorkOrder wo = new WorkOrder();
            wo.setStatus(InspectionStatuses.WO_PENDING);
            wo.setOrderNumber(orderNumber);
            wo.setType(WorkOrderTypes.DistributionLineInspection);
            wo.setRequestDate(LocalDate.now());
            data.addWorkOrder(wo, true);
            data.setCurrentWorkOrder(wo);
        }
        
        // Cache sites
        // feederNumber: "N5274541201"
        // feeder Directory: RUSHVILLE (454) 1201
        // Parse numbers in feeder directory so that we get 4541201 and match it to feeder number.
        FeederSearchParams fparams = new FeederSearchParams();
        fparams.setOrganizationId(DUKE_ORG_ID);
        try {
            for (Feeder f : svcs.feeders().search(svcs.token(), fparams)) {
                data.getFeedersByFeederNum().put(f.getFeederNumber().substring(f.getFeederNumber().length()-7), f);
                data.getDomainObjectIsNew().put(f.getId(), false);
                data.getCurrentWorkOrder().ensureSiteId(f.getId());
                if (!DataImportUtilities.ensureFeederInspection(svcs, data, listener, f, InspectionStatuses.SI_PENDING)) {
                    listener.reportFatalError(String.format("Unable to create feeder inspection for feeder %s", f.getId()));
                    return;
                } else {
                    data.getFeederInspections().put(f.getId(), data.getCurrentFeederInspection());
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException("Unable to load feeders for Duke", ex);
        }
        
        Pole pole;
        PoleInspection pinsp;
        PoleSearchParams pparams = new PoleSearchParams();
        ResourceSearchParams rparams = new ResourceSearchParams();
        ResourceMetadata rmeta;
        // for each priority directory
        for (File priorityDir : dir.listFiles(FileFilters.DIRECTORY_FILTER)) {
            // for each site directory
            for (File siteDir : priorityDir.listFiles(FileFilters.DIRECTORY_FILTER)) {
                String feederKey = siteDir.getName();
                String[] parts = feederKey.split(" ");
                feederKey = "";
                for (int i = parts.length - 2; i < parts.length; i++) {
                        feederKey = feederKey + parts[i].replace("(", "").replace(")", "");
                }
                feeder = data.getFeedersByFeederNum().get(feederKey);
                if (feeder == null) {
                    listener.reportNonFatalError(String.format("Unable to locate feeder %s", feederKey));
                    continue;
                } else {
                    data.setCurrentFeeder(feeder);
                    data.setCurrentFeederInspection(data.getFeederInspections().get(feeder.getId()));
                    if (data.getCurrentFeederInspection() == null) {
                        // Shouldn't happen, but I like to check
                        listener.reportNonFatalError(String.format("Unable to locate inspection for feeder %s", feederKey));
                        continue;
                    }
                }
                pparams.setSiteId(feeder.getId());
                // for each pole directory
                for (File poleDir : siteDir.listFiles(FileFilters.DIRECTORY_FILTER)) {
                    // the directory name should be the pole's serial number.
                    pparams.setSerialNumber(poleDir.getName());
                    try {
                        pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
                        if (pole == null) {
                            listener.reportNonFatalError(String.format("Unable to locate pole %s for site %s", pparams.getSerialNumber(), pparams.getSiteId()));
                        } else {
                            if (pole.getName() == null) {
                                pole.setName(pole.getSerialNumber());
                            }
                            pinsp = null;
                            for (File imageFile : poleDir.listFiles(FileFilters.IMAGES_FILTER)) {
                                try {
                                    String name = imageFile.getName();
                                    rparams.setAssetId(pole.getId());
                                    rparams.setName(name);
                                    rmeta = CollectionsUtilities.firstItemIn(svcs.resources().search(svcs.token(), rparams));
                                    if (rmeta == null) {
                                        rmeta = new ResourceMetadata();
                                        rmeta.setResourceId(UUID.randomUUID().toString());
                                        // Default to manual unless we can determine it's drone
                                        rmeta.setType(ResourceTypes.ManualInspectionImage);
                                        TiffImageMetadata exif;
                                        ImageInfo info = Imaging.getImageInfo(imageFile);
                                        ImageMetadata metadata = Imaging.getMetadata(imageFile);
                                        if (metadata instanceof JpegImageMetadata) {
                                            exif = ((JpegImageMetadata)metadata).getExif();
                                        } else if (metadata instanceof TiffImageMetadata) {
                                            exif = (TiffImageMetadata)metadata;
                                        } else {
                                            exif = null;
                                        }
                                        if (exif != null) {
                                            // Other than looking at make, we could look for roll, yaw, or some other drone-specific
                                            // GPS attribute. However, it's safe to assume if it's a DJI camera, it's drone.
                                            String make = ImageUtilities.getCameraMake(exif);
                                            if (DRONE_CAMERA_MAKE.equalsIgnoreCase(make)) {
                                                rmeta.setType(ResourceTypes.DroneInspectionImage);
                                            }
                                        }
                                        listener.reportMessage(String.format("\tType %s determined for image file %s", rmeta.getType().getValue(), name));
                                        rmeta.setTimestamp(ImageUtilities.getTimestamp(exif, DEFAULT_TZ));
                                        if (pinsp == null) {
                                            LocalDate inspectionDate = rmeta.getTimestamp() == null ? LocalDate.now() : rmeta.getTimestamp().toLocalDate();
                                            pinsp = DataImportUtilities.ensurePoleInspection(svcs, listener, data, pole, inspectionDate);
                                            if (pinsp == null) {
                                                listener.reportFatalError(String.format("Unable to create inspection for pole %s", pole.getSerialNumber()));
                                                return;
                                            } else if (pinsp.getDateOfInspection() == null) {
                                                pinsp.setDateOfInspection(inspectionDate);
                                            }
                                            if (
                                                    pinsp.getStatus() == null || (
                                                        !pinsp.getStatus().equals(InspectionStatuses.AI_COMPLETE) &&
                                                        !pinsp.getStatus().equals(InspectionStatuses.AI_PENDING_MERGE) &&
                                                        !pinsp.getStatus().equals(InspectionStatuses.AI_PROCESSED)
                                                    )
                                               )
                                            {
                                                pinsp.setStatus(InspectionStatuses.AI_PROCESSED);
                                            }
                                        }
                                        rmeta.setAssetId(pole.getId());
                                        rmeta.setAssetInspectionId(pinsp.getId());
                                        rmeta.setContentType(info.getMimeType());
                                        rmeta.setLocation(ImageUtilities.getLocation(exif));
                                        rmeta.setName(name);
                                        rmeta.setOrderNumber(pinsp.getOrderNumber());
                                        rmeta.setSize(ImageUtilities.getSize(info));
                                        rmeta.setStatus(ResourceStatus.QueuedForUpload);
                                        rmeta.setSiteId(pole.getSiteId());
                                        rmeta.setSiteInspectionId(pinsp.getSiteInspectionId());
                                        rmeta.setTimestamp(ImageUtilities.getTimestamp(exif, DEFAULT_TZ));
                                        data.addResourceMetadata(rmeta, imageFile, true);
                                    } else {
                                        data.addResourceMetadata(rmeta, imageFile, false);
                                    }
                                } catch (ImageReadException ex) {
                                    listener.reportNonFatalException(String.format("Unable to process image %s.  It will be skipped.", imageFile), ex);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        listener.reportNonFatalException(String.format("Error processing %s", poleDir.getAbsolutePath()), ex);
                    }
                }
            }
        }
        
        //TODO: Update statuses for inspection objects
        
        // Now we can save data
        try {
            boolean success = DataImportUtilities.saveData(svcs, listener, data);
            success = success && DataImportUtilities.saveResources(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException("Error saving data %s", ex);
        }
    }
}
