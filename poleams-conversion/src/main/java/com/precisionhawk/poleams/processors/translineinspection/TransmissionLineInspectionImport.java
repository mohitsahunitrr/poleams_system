package com.precisionhawk.poleams.processors.translineinspection;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ImagePosition;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
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
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.FileFilters;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.poleams.webservices.ResourceWebService;
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
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 * Imports data for a transmission line inspection.
 *
 * @author pchapman
 */
public final class TransmissionLineInspectionImport {
    
    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/New_York");

    private static File findMasterSurveyTemplate(ProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(FileFilters.EXCEL_SPREADSHEET_FILTER);
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
        data.setCurrentOrderNumber(orderNumber);
        try {
            boolean success = initialize(wsclient, listener, data);
            success = success && parseStructureData(wsclient, listener, inputDir, data);
            listener.reportMessage(String.format("Import completed %s serrors", success ? "without" : "with"));
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        }
    }

    private boolean initialize(WSClientHelper wsclient, ProcessListener listener, InspectionData data) throws IOException {
        try {
            data.setCurrentWorkOrder(wsclient.workOrders().retrieveById(wsclient.token(), data.getCurrentOrderNumber()));
        } catch (ClientResponseFailure ex) {
            if (ex.getResponse().getStatus() == 404) {
                // it does not exist.
            } else {
                throw new IOException("Unable to lookup work order.", ex);
            }
        }
        if (data.getCurrentWorkOrder() == null) {
            WorkOrder wo = new WorkOrder();
            wo.setOrderNumber(data.getCurrentOrderNumber());
            wo.setStatus(WorkOrderStatuses.Requested);
            wo.setType(WorkOrderTypes.TransmissionLineInspection);
            data.setCurrentWorkOrder(wo);
            data.getDomainObjectIsNew().put(data.getCurrentOrderNumber(), true);
        } else {
            data.getDomainObjectIsNew().put(data.getCurrentOrderNumber(), false);
        }
        return true;
    }

    private boolean parseStructureData(WSClientHelper wsclient, ProcessListener listener, File inputDir, InspectionData data) {
        File excelFile = findMasterSurveyTemplate(listener, inputDir);
        Workbook workbook = null;
        try {
            if (excelFile != null) {
                workbook = XSSFWorkbookFactory.createWorkbook(excelFile, true);

                // Find and process the "Survey Data" sheet.
                Sheet sheet = workbook.getSheetAt(0);

                boolean dataFound = true;
                for (int rowIndex = 1; dataFound; rowIndex++) {
                    dataFound = processStructureRow(wsclient, listener, sheet.getRow(rowIndex), data);
                }
            }
            
            // Process images
            File imagesDir;
            for (TransmissionStructure struct : data.getStructuresMap().values()) {
                imagesDir = new File(inputDir, struct.getStructureNumber());
                if (imagesDir.isDirectory()) {
                    File[] files = imagesDir.listFiles(FileFilters.IMAGES_FILTER);
                    for (File imageFile : files) {
                        if (imageFile.isFile()) {
                            if (imageFile.canRead()) {
                                try {
                                    ImageFormat format = Imaging.guessFormat(imageFile);
                                    if (ImageFormats.UNKNOWN.equals(format)) {
                                        listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", imageFile));
                                    } else {
                                        processImageFile(wsclient, listener, data, struct, imageFile, format);
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
                } else {
                    listener.reportNonFatalError(String.format("Images directory \"%s\" not found.", imagesDir.getAbsolutePath()));
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
    
    private static final int COL_STRUCT_NUM = 0;
    private static final int COL_LINE = 1;
    private static final int COL_STRUCT_NAME = 2;
    private static final int COL_INSP_DATE = 3;
    private static final int COL_LATITUDE = 4;
    private static final int COL_LONGITUDE = 5;
    private static final int COL_REASON_NO_INSP = 6;
    private static final int COL_MATERIAL = 7;

    private boolean processStructureRow(WSClientHelper wsclient, ProcessListener listener, Row row, InspectionData data)
        throws InvalidFormatException, IOException
    {
        String lineId = getCellDataAsString(row, COL_LINE);
        if (lineId == null || lineId.isEmpty()) {
             return false;
        }
        String structNum = getCellDataAsString(row, COL_STRUCT_NUM);
        structNum = structNum.replace(".0", "").replace(" Pending Replacement", "");
        Date inspectionDate = getCellDataAsDate(row, COL_INSP_DATE);
        Double latitude = getCellDataAsDouble(row, COL_LATITUDE);
        Double longitude = getCellDataAsDouble(row, COL_LONGITUDE);
        if (longitude != null) {
            longitude = longitude * -1.0;
        }
        String reasonNoInsp = getCellDataAsString(row, COL_REASON_NO_INSP);
        String material = getCellDataAsString(row, COL_MATERIAL);
        String lineName = getCellDataAsString(row, COL_STRUCT_NAME);
        
        listener.reportMessage(String.format("Processing row %d for transmission structure %s", row.getRowNum(), structNum));
        
        // Transmission Line
        if (data.getCurrentLine() == null) {
            TransmissionLineSearchParams params = new TransmissionLineSearchParams();
            params.setLineNumber(lineId);
            params.setOrganizationId(data.getOrganizationId());
            data.setCurrentLine(CollectionsUtilities.firstItemIn(wsclient.transmissionLines().search(wsclient.token(), params)));
            if (data.getCurrentLine() == null) {
                // Not found, create it.
                TransmissionLine line = new TransmissionLine();
                line.setId(UUID.randomUUID().toString());
                line.setName(lineName);
                line.setLineNumber(lineId);
                line.setOrganizationId(data.getOrganizationId());
                data.setCurrentLine(line);
                // It must be saved early due to security authentication in the services.
                wsclient.transmissionLines().create(wsclient.token(), line);
                data.getDomainObjectIsNew().put(line.getId(), false);
            } else {
                data.getCurrentLine().setOrganizationId(data.getOrganizationId());
                data.getDomainObjectIsNew().put(data.getCurrentLine().getId(), false);
            }
        }
        boolean found = false;
        for (String id : data.getCurrentWorkOrder().getSiteIds()) {
            if (data.getCurrentLine().getId().equals(id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            data.getCurrentWorkOrder().getSiteIds().add(data.getCurrentLine().getId());
        }
        
        // Transmission Line Inspection
        if (data.getCurrentLineInspection() == null) {
            SiteInspectionSearchParams params = new SiteInspectionSearchParams();
            params.setOrderNumber(data.getCurrentOrderNumber());
            params.setSiteId(data.getCurrentLine().getId());
            data.setCurrentLineInspection(CollectionsUtilities.firstItemIn(wsclient.transmissionLineInspections().search(wsclient.token(), params)));
            if (data.getCurrentLineInspection() == null) {
                TransmissionLineInspection insp = new TransmissionLineInspection();
                if (inspectionDate != null) {
                    insp.setDateOfInspection(LocalDate.of(inspectionDate.getYear(), inspectionDate.getMonth(), inspectionDate.getDay())); //TODO: handle this better
                }
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getCurrentOrderNumber());
                insp.setSiteId(data.getCurrentLine().getId());
                data.setCurrentLineInspection(insp);
                data.getDomainObjectIsNew().put(insp.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getCurrentLineInspection().getId(), false);
            }
        }
        
        // Transmission Structure
        TransmissionStructureSearchParams tssp = new TransmissionStructureSearchParams();
        tssp.setSiteId(data.getCurrentLine().getId());
        tssp.setStructureNumber(structNum);
        TransmissionStructure struct = CollectionsUtilities.firstItemIn(wsclient.transmissionStructures().search(wsclient.token(), tssp));
        if (struct == null) {
            struct = new TransmissionStructure();
            struct.setId(UUID.randomUUID().toString());
            struct.setSiteId(data.getCurrentLine().getId());
            struct.setStructureNumber(structNum);
            if ("Concrete".equalsIgnoreCase(material)) {
                struct.setType(AssetTypes.ConcretePole);
            } else if ("Wood".equalsIgnoreCase(material)) {
                struct.setType(AssetTypes.WoodenPole);
            } else if ("Metal".equalsIgnoreCase(material)) {
                struct.setType(AssetTypes.SteelTower);
            }
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
        aisp.setOrderNumber(data.getCurrentOrderNumber());
        aisp.setSiteId(data.getCurrentLine().getId());
        aisp.setSiteInspectionId(data.getCurrentLineInspection().getId());
        TransmissionStructureInspection insp = CollectionsUtilities.firstItemIn(wsclient.transmissionStructureInspections().search(wsclient.token(), aisp));
        if (insp == null) {
            insp = new TransmissionStructureInspection();
            insp.setAssetId(struct.getId());
            if (inspectionDate != null) {
                insp.setDateOfInspection(LocalDate.of(inspectionDate.getYear(), inspectionDate.getMonth(), inspectionDate.getDay())); //TODO: handle this better
            }
            insp.setId(UUID.randomUUID().toString());
            insp.setOrderNumber(data.getCurrentOrderNumber());
            if (reasonNoInsp != null && !reasonNoInsp.isEmpty()) {
                insp.setReasonNotInspected(reasonNoInsp);
            }
            insp.setSiteId(data.getCurrentLine().getId());
            insp.setSiteInspectionId(data.getCurrentLineInspection().getId());
            insp.setType(TransmissionStructureInspectionTypes.DroneInspection);
            data.addTransmissionStructureInspection(struct, insp, true);
        } else {
            data.addTransmissionStructureInspection(struct, insp, false);
        }
        
        return true;
    }

    private void processImageFile(WSClientHelper wsclient, ProcessListener listener, InspectionData data, TransmissionStructure struct, File f, ImageFormat format)
        throws IOException, ImageReadException
    {
        listener.reportMessage(String.format("Processing image file %s", f.getAbsolutePath()));
        
        ResourceWebService rsvc = wsclient.resources();
                
        ResourceSearchParams params = new ResourceSearchParams();
        params.setAssetId(struct.getId());
        params.setName(f.getName());
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(rsvc.search(wsclient.token(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            rmeta.setResourceId(UUID.randomUUID().toString());
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
            rmeta.setOrderNumber(data.getCurrentOrderNumber());
            String posSide = resourcePositionSide(listener, f);
            if (posSide != null) {
                ImagePosition pos = new ImagePosition();
                pos.setSide(posSide);
                rmeta.setPosition(pos);
            }
            rmeta.setAssetId(struct.getId());
            rmeta.setAssetInspectionId(data.getStructureInspectionsMap().get(new SiteAssetKey(struct)).getId());
            rmeta.setSize(ImageUtilities.getSize(info));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(data.getCurrentLine().getId());
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
            // Save Data
            listener.reportMessage("Saving data");
            DataImportUtilities.saveData(wsclient, listener, data);
            
            // Save Resources
            listener.reportMessage("Uploading line resources.");
            DataImportUtilities.saveResources(wsclient, listener, data);
        } catch (Throwable t) {
            listener.reportFatalException("Error persisting inspection data.", t);
        }
    }
    
    private static String resourcePositionSide(ProcessListener listener, File file) {
        String part = file.getName().split("_")[1].split("\\.")[0];
        if (part.matches("[Aa]\\d*")) {
            return "A";
        } else if (part.matches("[Bb]\\d*")) {
            return "B";
        } else if (part.matches("[Cc]\\d*")) {
            return "C";
        } else if (part.matches("[Dd]\\d*")) {
            return "D";
        } else if (part.matches("\\d*[Oo][Vv][Ee][Rr][Vv][Ii][Ee][Ww]")) { // Overview
            return "Overview";
        } else if (part.matches("[Xx][Aa][Rr][Mm][Ss]\\d*")) { // Xarms
            return "Crossarms";
        } else {
            listener.reportMessage(String.format("Returning position side %s for file %s", "Other", file.getAbsolutePath()));
            return "Other";
        }
    }
}
