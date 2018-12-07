package com.precisionhawk.poleams.processors.translineinspection;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.AssetTypes;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.domain.TransmissionStructureInspectionTypes;
import com.precisionhawk.poleams.domain.WorkOrderStatuses;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.FilenameFilters;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ResourceDataUploader;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;

/**
 * Imports data for a transmission line inspection.
 *
 * @author pchapman
 */
public final class TransmissionLineInspectionImport {
    
    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/New_York");

    private static File findMasterSurveyTemplate(ProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(FilenameFilters.EXCEL_SPREADSHEET_FILTER);
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple excel files exist in directory \"%s\"", feederDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("Master Survey Template does not exist in directory \"%s\" or is not readable", feederDir));
            return null;
        } else {
            return files[0];
        }
    }
    
    public void process(Environment environment, ProcessListener listener, String orgId, String orderNumber, File inputDir) {
        WSClientHelper wsclient = new WSClientHelper(environment);
        InspectionData data = new InspectionData();
        data.setOrganizationId(orgId);
        data.setOrderNumber(orderNumber);
        try {
            boolean success = initialize(wsclient, listener, data);
            success = success && parseStructureData(wsclient, listener, inputDir, data);
            listener.reportMessage(String.format("Import completed %s serrors", success ? "without" : "with"));
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        }
    }

    private boolean initialize(WSClientHelper wsclient, ProcessListener listener, InspectionData data) throws IOException {
        data.setWorkOrder(wsclient.workOrders().retrieveById(wsclient.token(), data.getOrderNumber()));
        if (data.getWorkOrder() == null) {
            WorkOrder wo = new WorkOrder();
            wo.setOrderNumber(data.getOrderNumber());
            wo.setStatus(WorkOrderStatuses.Requested);
            wo.setType(WorkOrderTypes.TransmissionLineInspection);
            data.setWorkOrder(wo);
            data.getDomainObjectIsNew().put(data.getOrderNumber(), true);
        } else {
            data.getDomainObjectIsNew().put(data.getOrderNumber(), false);
        }
        return true;
    }

    private boolean parseStructureData(WSClientHelper wsclient, ProcessListener listener, File inputDir, InspectionData data) {
        File excelFile = findMasterSurveyTemplate(listener, inputDir);
        if (excelFile == null) {
            return false;
        }
        Workbook workbook = null;
        try {
            workbook = XSSFWorkbookFactory.createWorkbook(excelFile, true);
            
            // Find and process the "Survey Data" sheet.
            Sheet sheet = workbook.getSheetAt(0);
            
            boolean dataFound = true;
            for (int rowIndex = 1; dataFound; rowIndex++) {
                dataFound = processPoleRow(wsclient, listener, sheet.getRow(rowIndex), data);
            }
            
            // Process images
            File[] files = inputDir.listFiles(FilenameFilters.IMAGES_FILTER);
            for (File imageFile : files) {
                if (imageFile.isFile()) {
                    if (imageFile.canRead()) {
                        try {
                            ImageFormat format = Imaging.guessFormat(imageFile);
                            if (ImageFormats.UNKNOWN.equals(format)) {
                                listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", imageFile));
                            } else {
                                processImageFile(wsclient, listener, data, imageFile, format);
                            }
                        } catch (ImageReadException | IOException ex) {
                            listener.reportNonFatalException(String.format("There was an error parsing resource file \"%s\"", imageFile.getAbsolutePath()), ex);

                            return true;
                        }
                    } else {
                        listener.reportNonFatalError(String.format("The file \"%s\" is not readable.", imageFile));
                    }
                } else {
                    listener.reportMessage(String.format("The directory \"%s\" is being ignored.", imageFile));
                }
            }
            
            saveData(wsclient, listener, data);
            
            return true;
        } catch (InvalidFormatException | IOException ex) {
            listener.reportFatalException(ex);
            return false;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException ioe) {
                    listener.reportNonFatalException("Unable to close master survey template.", ioe);
                }
            }
        }        
    }

    private boolean processPoleRow(WSClientHelper wsclient, ProcessListener listener, Row row, InspectionData data)
        throws InvalidFormatException, IOException
    {
        String lineId = getCellDataAsString(row, 0);
        if (lineId == null || lineId.isEmpty()) {
             return false;
        }
        String structNum = getCellDataAsString(row, 1);
        structNum = structNum.replace(".0", "").replace(" Pending Replacement", "");
        Date inspectionDate = getCellDataAsDate(row, 2);
        Double latitude = getCellDataAsDouble(row, 3);
        Double longitude = getCellDataAsDouble(row, 4);
        if (longitude != null) {
            longitude = longitude * -1.0;
        }
        String reasonNoInsp = getCellDataAsString(row, 5);
        
        listener.reportMessage(String.format("Processing row %d for transmission structure %s", row.getRowNum(), structNum));
        
        // Transmission Line
        if (data.getLine() == null) {
            TransmissionLineSearchParams params = new TransmissionLineSearchParams();
            params.setLineNumber(lineId);
            data.setLine(CollectionsUtilities.firstItemIn(wsclient.transmissionLines().search(wsclient.token(), params)));
            if (data.getLine() == null) {
                // Not found, create it.
                TransmissionLine line = new TransmissionLine();
                line.setId(UUID.randomUUID().toString());
                line.setName(lineId);
                line.setLineNumber(lineId);
                line.setOrganizationId(data.getOrganizationId());
                data.setLine(line);
                // It must be saved early due to security authentication in the services.
                wsclient.transmissionLines().create(wsclient.token(), line);
                data.getDomainObjectIsNew().put(line.getId(), false);
            } else {
                data.getLine().setOrganizationId(data.getOrganizationId());
                data.getDomainObjectIsNew().put(data.getLine().getId(), false);
            }
        }
        boolean found = false;
        for (String id : data.getWorkOrder().getSiteIds()) {
            if (data.getLine().getId().equals(id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            data.getWorkOrder().getSiteIds().add(data.getLine().getId());
        }
        
        // Transmission Line Inspection
        if (data.getLineInspection() == null) {
            SiteInspectionSearchParams params = new SiteInspectionSearchParams();
            params.setOrderNumber(data.getOrderNumber());
            params.setSiteId(data.getLine().getId());
            data.setLineInspection(CollectionsUtilities.firstItemIn(wsclient.transmissionLineInspections().search(wsclient.token(), params)));
            if (data.getLineInspection() == null) {
                TransmissionLineInspection insp = new TransmissionLineInspection();
                if (inspectionDate != null) {
                    insp.setDateOfInspection(LocalDate.of(inspectionDate.getYear(), inspectionDate.getMonth(), inspectionDate.getDay())); //TODO: handle this better
                }
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getLine().getId());
                data.setLineInspection(insp);
                data.getDomainObjectIsNew().put(insp.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getLineInspection().getId(), false);
            }
        }
        
        // Transmission Structure
        TransmissionStructureSearchParams tssp = new TransmissionStructureSearchParams();
        tssp.setSiteId(data.getLine().getId());
        tssp.setStructureNumber(structNum);
        TransmissionStructure struct = CollectionsUtilities.firstItemIn(wsclient.transmissionStructures().search(wsclient.token(), tssp));
        if (struct == null) {
            struct = new TransmissionStructure();
            struct.setId(UUID.randomUUID().toString());
            struct.setSiteId(data.getLine().getId());
            struct.setStructureNumber(structNum);
            struct.setType(AssetTypes.WoodenPole);
            if (latitude != null && longitude != null) {
                GeoPoint p = new GeoPoint();
                p.setLatitude(latitude);
                p.setLongitude(longitude);
                if (p.getLatitude() != null && p.getLongitude() != null) {
                    struct.setLocation(p);
                }
            }
            data.addTransmissionStruture(struct, true);
        } else {
            data.addTransmissionStruture(struct, false);
        }
        
        // Transmission Structure Inspection
        AssetInspectionSearchParams aisp = new AssetInspectionSearchParams();
        aisp.setAssetId(struct.getId());
        aisp.setOrderNumber(data.getOrderNumber());
        aisp.setSiteId(data.getLine().getId());
        aisp.setSiteInspectionId(data.getLineInspection().getId());
        TransmissionStructureInspection insp = CollectionsUtilities.firstItemIn(wsclient.transmissionStructureInspections().search(wsclient.token(), aisp));
        if (insp == null) {
            insp = new TransmissionStructureInspection();
            insp.setAssetId(struct.getId());
            if (inspectionDate != null) {
                insp.setDateOfInspection(LocalDate.of(inspectionDate.getYear(), inspectionDate.getMonth(), inspectionDate.getDay())); //TODO: handle this better
            }
            insp.setId(UUID.randomUUID().toString());
            insp.setOrderNumber(data.getOrderNumber());
            if (reasonNoInsp != null && !reasonNoInsp.isEmpty()) {
                insp.setReasonNotInspected(reasonNoInsp);
            }
            insp.setSiteId(data.getLine().getId());
            insp.setSiteInspectionId(data.getLineInspection().getId());
            insp.setType(TransmissionStructureInspectionTypes.DroneInspection);
            data.addTransmissionStructureInspection(struct, insp, true);
        } else {
            data.addTransmissionStructureInspection(struct, insp, false);
        }
        
        return true;
    }

    private void processImageFile(WSClientHelper wsclient, ProcessListener listener, InspectionData data, File f, ImageFormat format)
        throws IOException, ImageReadException
    {
        // Expect a name like "30_WP right pole.jpg" where 30 is the structure number.
        String[] parts = f.getName().split("_");
        TransmissionStructure struct = data.getStructureDataByStructureNum().get(parts[0]);
        if (struct == null) {
            listener.reportNonFatalError(String.format("Unable to load structure %s for file %s.", parts[0], f.getAbsolutePath()));
            return;
        }
        
        ResourceWebService rsvc = wsclient.resources();
                
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetId(struct.getId());
        params.setName(f.getName());
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.query(wsclient.token(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            rmeta.setResourceId(UUID.randomUUID().toString());
            String name = f.getName().toLowerCase();
            rmeta.setType(ResourceTypes.DroneInspectionImage);
            ImageInfo info = Imaging.getImageInfo(f);
            ImageMetadata metadata = Imaging.getMetadata(f);
            TiffImageMetadata exif;
            if (metadata instanceof JpegImageMetadata) {
                exif = ((JpegImageMetadata)metadata).getExif();
            } else if (metadata instanceof TiffImageMetadata) {
                exif = (TiffImageMetadata)metadata;
            } else {
                exif = null;
            }
            rmeta.setContentType(info.getMimeType());
            rmeta.setLocation(ImageUtilities.getLocation(exif));
            rmeta.setName(f.getName());
            rmeta.setOrderNumber(data.getOrderNumber());
            rmeta.setAssetId(struct.getId());
            rmeta.setAssetInspectionId(data.getStructureInspectionsByStructureNum().get(struct.getStructureNumber()).getId());
            rmeta.setSize(ImageUtilities.getSize(info));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(data.getLine().getId());
            rmeta.setTimestamp(ImageUtilities.getTimestamp(exif, DEFAULT_TZ));
            data.addResourceMetadata(rmeta, f, true);
        } else {
            List<String> resourceIDs = new LinkedList<>();
            resourceIDs.add(rmeta.getResourceId());
            Map<String, Boolean> results = rsvc.verifyUploadedResources(wsclient.token(), resourceIDs);
            if (
                    rmeta.getStatus() == ResourceStatus.QueuedForUpload
                    || (!results.get(rmeta.getResourceId()))
                )
            {
                // Add it to the list so that the upload is attempted again
                data.addResourceMetadata(rmeta, f, false);
            }
        }
    }

    private void saveData(WSClientHelper wsclient, ProcessListener listener, InspectionData data) {
        try {
            listener.reportMessage("Saving data");

            // Save Line
            if (data.getLine() != null) {
                TransmissionLineWebService svc = wsclient.transmissionLines();
                if (data.getDomainObjectIsNew().get(data.getLine().getId())) {
                    svc.create(wsclient.token(), data.getLine());
                    listener.reportMessage(String.format("Inserted new line %s", data.getLine().getLineNumber()));
                } else {
                    svc.update(wsclient.token(), data.getLine());
                    listener.reportMessage(String.format("Updated feeder %s", data.getLine().getLineNumber()));
                }
            }
            
            // Save Work Order
            if (data.getWorkOrder() != null) {
                WorkOrderWebService svc = wsclient.workOrders();
                if (data.getDomainObjectIsNew().get(data.getWorkOrder().getOrderNumber())) {
                    svc.create(wsclient.token(), data.getWorkOrder());
                    listener.reportMessage(String.format("Inserted new work order %s", data.getWorkOrder().getOrderNumber()));
                } else {
                    svc.update(wsclient.token(), data.getWorkOrder());
                    listener.reportMessage(String.format("Updated work order %s", data.getWorkOrder().getOrderNumber()));
                }
            }
            
            // Save Line Inspection
            if (data.getLineInspection() != null) {
                TransmissionLineInspectionWebService svc = wsclient.transmissionLineInspections();
                if (data.getDomainObjectIsNew().get(data.getLineInspection().getId())) {
                    svc.create(wsclient.token(), data.getLineInspection());
                    listener.reportMessage(String.format("Inserted new line inspection %s", data.getLineInspection().getId()));
                } else {
                    svc.update(wsclient.token(), data.getLineInspection());
                    listener.reportMessage(String.format("Updated line inspection %s", data.getLineInspection().getId()));
                }
            }

            // Save Structures
            if (!data.getStructureDataByStructureNum().isEmpty()) {
                TransmissionStructureWebService psvc = wsclient.transmissionStructures();
                for (TransmissionStructure pdata : data.getStructureDataByStructureNum().values()) {
                    if (data.getDomainObjectIsNew().get(pdata.getId())) {
                        psvc.create(wsclient.token(), pdata);
                        listener.reportMessage(String.format("Inserted new transmission structure %s structure num %s", pdata.getId(), pdata.getStructureNumber()));
                    } else {
                        psvc.update(wsclient.token(), pdata);
                        listener.reportMessage(String.format("Updated transmission structure %s structure num %s", pdata.getId(), pdata.getStructureNumber()));
                    }
                }
            }

            // Save Structure Inspections
            if (!data.getStructureInspectionsByStructureNum().isEmpty()) {
                TransmissionStructureInspectionWebService pisvc = wsclient.transmissionStructureInspections();
                for (TransmissionStructureInspection pi : data.getStructureInspectionsByStructureNum().values()) {
                    if (data.getDomainObjectIsNew().get(pi.getId())) {
                        pisvc.create(wsclient.token(), pi);
                        listener.reportMessage(String.format("Inserted new inspection for structure %s", pi.getAssetId()));
                    } else {
                        pisvc.update(wsclient.token(), pi);
                        listener.reportMessage(String.format("updated inspection for structure %s", pi.getAssetId()));
                    }
                }
            }
            
            // Save Resources
            listener.reportMessage("Uploading line resources.");
            ResourceWebService rsvc = wsclient.resources();
            for (ResourceMetadata rmeta : data.getFeederResources()) {
                ResourceDataUploader.uploadResources(wsclient.getEnv(), listener, data, data.getFeederResources(), data.getResourceDataFiles(), 2);
            }
            int index = 1;
            for (List<ResourceMetadata> list : data.getPoleResources().values()) {
                listener.reportMessage(String.format("Uploading transmission structure resources ( %d of %d structures).", index++, data.getPoleResources().size()));
                ResourceDataUploader.uploadResources(wsclient.getEnv(), listener, data, list, data.getResourceDataFiles(), 2);
            }
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
    }
}
