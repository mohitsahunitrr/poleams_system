package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.domain.WorkOrderStatus;
import com.precisionhawk.ams.domain.WorkOrderType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.papernapkin.liana.util.StringUtil;

/**
 * A processor that expects a few things:
 * <ul>
 * <li>1 or more CSV files that are named in the format &quot;substationname_feedernum_[Priority]Poles_yymmdd.csv&quot;</li>
 * <li>The CSV files will have 10 columns</li>
 * <li>Column 6 will be FPLID, Column 9 will be Latitude, 10 Longitude</li>
 * <li>Subdirectories full of images, i pole per directory.</li>
 * <li>Pole image subdirectory has the name {sequence}_{fplid} (last portion may be optional if no matching fplid)</li>
 * <li>Ground images have the letters &quot;GC&quot; in the name.</li>
 * <li>RGB images have the letters &quot;RGB&quot; in the name.</li>
 * <li>Thermal images have the letters &quot;Therm&quot; in the name.</li>
 * </ul>
 *
 * @author pchapman
 */
public class FeederDataDirProcessor2 {
    
    private static final int COL_FPLID = 5;
    private static final int COL_LAT = 8;
    private static final int COL_LON = 9;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final FileFilter DIR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }  
    };
    
    private static final FilenameFilter CSV_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".csv");
        }
    };
    
    private static final FilenameFilter GROUND_IMG_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toUpperCase().contains("GC");
        }
    };
    
    private static final FilenameFilter RPG_IMG_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            String s = name.toUpperCase();
            return s.contains("RGB") || s.contains("RBG"); // Catch types with "RBG"
        }
    };
    
    private static final FilenameFilter THERMAL_IMG_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toUpperCase().contains("THERM");
        }
    };
    
    static final ImagesProcessor IMAGES_PROCESSOR;
    static {
        IMAGES_PROCESSOR = new ImagesProcessor();
        IMAGES_PROCESSOR.setDroneImageFilter(RPG_IMG_FILE_FILTER);
        IMAGES_PROCESSOR.setManualImageFilter(GROUND_IMG_FILE_FILTER);
        IMAGES_PROCESSOR.setThermalImageFilter(THERMAL_IMG_FILE_FILTER);
    }
    
    // no state
    private FeederDataDirProcessor2() {};

    public static boolean process(Environment env, ProcessListener listener, File feederDir, String orgId, String orderNumber) {
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setOrderNumber(orderNumber);
        data.setOrganizationId(orgId);
        // find and process CSV files.
        boolean success = true;
        try {
            success = ensureWorkOrder(svcs, data, listener);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            success = false;
        }
        if (success) {
            for (File f : feederDir.listFiles(CSV_FILE_FILTER)) {
                success = success && processCSVFile(svcs, data, listener, f);
            }
        }
        if (success) {
            // find and process pole dirs.
            for (File f : feederDir.listFiles(DIR_FILTER)) {
                processPoleDir(svcs, listener, data, f);
            }
        }
        try {
            success = success && DataImportUtilities.saveData(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            success = false;
        }
        try {
            success = success && DataImportUtilities.saveResources(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            success = false;
        }
        
        return success;
    }

    private static boolean processCSVFile(WSClientHelper svcs, InspectionData data, ProcessListener listener, File f) {
        listener.reportMessage(String.format("Processing CSV file %s", f.getName()));
        String fplId;
        Reader in = null;
        Float lat;
        Float lon;
        String s;
        LocalDate inspectionDate = null;
        try {
            in = new FileReader(f);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                if (record.getRecordNumber() == 1) {
                    // Skip 1st record other than checking for proper number of columns.
                    if (record.size() < 10) {
                        listener.reportMessage(String.format("The file \"%s\" has too few columns.  It is being skipped.", f.getName()));
                        return true;
                    }
                    listener.reportMessage("\tCSV row 1 has valid number of columns.  Skipping row 1");
                } else {
                    listener.reportMessage(String.format("\tProcessing %s record %d", f.getName(), record.getRecordNumber()));
                    if (data.getFeeder() == null) {
                        // Parse file name for feeder name and number.
                        String[] parts = f.getName().split("_");
                        String subStationName = parts[0];
                        String feederNum = parts[1];
//                        if ()
//                        parts = parts[3].split("\\.");
//                        String datePart = "20" + parts[0];
//                        inspectionDate = LocalDate.parse(datePart, DATE_FORMAT);
                        inspectionDate = LocalDate.now();
                        if (!ensureFeeder(svcs, data, listener, feederNum, subStationName)) return false;
                    }
                    fplId = record.get(COL_FPLID);
                    lat = getFloat(listener, record, COL_LAT);
                    lon = getFloat(listener, record, COL_LON);
                    Pole p = ensurePole(svcs, listener, data, fplId, inspectionDate);
                    if (p == null) {
                        return false;
                    }
                    if (lat != null && lon != null) {
                        GeoPoint loc = new GeoPoint();
                        loc.setLatitude(lat.doubleValue());
                        loc.setLongitude(lon.doubleValue());
                        p.setLocation(loc);
                    }
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }

    private static boolean processPoleDir(WSClientHelper svcs, ProcessListener listener, InspectionData data, File dir) {
        // Parse sequence and FPLID from file name.
        String[] parts = dir.getName().split("_");
        String seqStr = parts[0];
        String fplid = parts.length > 1 ? parts[1] : null;
        if (fplid == null) {
            //TODO: What to do here?
            return true;
        }
        if (parts.length > 2 || !StringUtil.isNumeric(seqStr) || !StringUtil.isNumeric(fplid)) {
            listener.reportMessage(String.format("Skipping directory %s due to invalid directory name.", dir.getName()));
        }
        Pole pole;
        PoleInspection insp;
        try {
            pole = ensurePole(svcs, listener, data, fplid, null);
            insp = ensurePoleInspection(svcs, listener, data, pole, null);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            return false;
        }
        pole.getAttributes().put("Sequence", seqStr);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormat.IMAGE_FORMAT_UNKNOWN.equals(format)) {
                            if (f.getName().endsWith("C.xml")) {
                                // PoleForman XML
                                PoleForemanXMLProcessor.process(listener, pole, insp, f);
                                addNonImageResource(svcs, data, data.getFeederInspection(), insp, ResourceTypes.PoleInspectionAnalysisXML, f, "application/xml");
                            } else if (f.getName().endsWith("C.pdf")) {
                                // PoleInspectionReport
                                addNonImageResource(svcs, data, data.getFeederInspection(), insp, ResourceTypes.PoleInspectionReport, f, "application/pdf");
                            } else if (f.getName().endsWith("_DSS.pdf")) {
                                // Drone Survey Sheet
                                addNonImageResource(svcs, data, data.getFeederInspection(), insp, ResourceTypes.PoleDroneSurveySheet, f, "application/pdf");
                            } else {
                                listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", f));
                            }
                        } else {
                            listener.reportMessage(String.format("Processing image file %s", f.getName()));
                            IMAGES_PROCESSOR.process(svcs.getEnv(), listener, data, pole, f, format);
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
    
    private static Float getFloat(ProcessListener listener, CSVRecord record, int col) {
        String s = record.get(col);
        try {
            if (s != null) {
                return Float.valueOf(s);
            }
        } catch (NumberFormatException ex) {}
        listener.reportNonFatalError(String.format("Invalid value.  Expecting a numiric value in column %d of row %d", col, record.getRecordNumber()));
        return null;
    }

    private static boolean ensureFeeder(WSClientHelper svcs, InspectionData data, ProcessListener listener, String feederNum, String subStationName) throws IOException {
        if (data.getFeeder() != null && data.getFeeder().getFeederNumber().equals(feederNum)) {
            return true;
        }
        FeederSearchParams params = new FeederSearchParams();
        params.setOrganizationId(data.getOrganizationId());
        params.setFeederNumber(feederNum);
        Feeder f = CollectionsUtilities.firstItemIn(svcs.feeders().search(svcs.token(), params));
        if (f == null) {
            f = new Feeder();
            f.setFeederNumber(feederNum);
            f.setName(subStationName);
            f.setOrganizationId(data.getOrganizationId());
            f.setId(UUID.randomUUID().toString());
            data.getDomainObjectIsNew().put(f.getId(), true);
        } else {
            data.getDomainObjectIsNew().put(f.getId(), false);
        }
        boolean found = false;
        for (String siteId : data.getWorkOrder().getSiteIds()) {
            if (f.getId().equals(siteId)) {
                found = true;
            }
        }
        if (!found) {
            data.getWorkOrder().getSiteIds().add(f.getId());
        }
        data.setFeeder(f);
        return true;
    }

    private static Pole ensurePole(WSClientHelper svcs, ProcessListener listener, InspectionData data, String fplid, LocalDate inspectionDate) throws IOException {
        
        Pole pole = data.getPoleDataByFPLId().get(fplid);
        if (pole != null) {
            return pole;
        }
        
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSiteId(data.getFeeder().getId());
        pparams.setUtilityId(fplid);
        pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
        AssetInspectionSearchParams iparams = null;
        if (pole == null) {
            pole = new Pole();
            pole.setUtilityId(fplid);
            pole.setId(UUID.randomUUID().toString());
            pole.setSiteId(data.getFeeder().getId());
            data.addPole(pole, true);
        } else {
            data.addPole(pole, false);
            iparams = new AssetInspectionSearchParams();
            iparams.setAssetId(pole.getId());
            iparams.setOrderNumber(data.getOrderNumber());
        }
        ensurePoleInspection(svcs, listener, data, pole, inspectionDate);
        return pole;
    }

    private static PoleInspection ensurePoleInspection(
            WSClientHelper svcs, ProcessListener listener, InspectionData data, Pole pole, LocalDate inspectionDate
        ) throws IOException
    {
        PoleInspection insp = data.getPoleInspectionsByFPLId().get(pole.getUtilityId());
        if (insp == null) {
            AssetInspectionSearchParams iparams = new AssetInspectionSearchParams();
            iparams.setAssetId(pole.getId());
            iparams.setOrderNumber(data.getOrderNumber());
            insp = CollectionsUtilities.firstItemIn(svcs.poleInspections().search(svcs.token(), iparams));
            if (insp == null) {
                insp = new PoleInspection();
                insp.setAssetId(pole.getId());
                insp.setDateOfInspection(inspectionDate);
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getFeeder().getId());
                insp.setStatus(new AssetInspectionStatus("Processed"));
                insp.setType(new AssetInspectionType("DistributionLineInspection"));
                data.addPoleInspection(pole, insp, true);
            } else {
                insp.setType(new AssetInspectionType("DistributionLineInspection"));
                data.addPoleInspection(pole, insp, false);
            }
        }
        return insp;
    }

    private static boolean ensureWorkOrder(WSClientHelper svcs, InspectionData data, ProcessListener listener) throws IOException {
        try {
            data.setWorkOrder(svcs.workOrders().retrieveById(svcs.token(), data.getOrderNumber()));
        } catch (ClientResponseFailure f) {
            if (f.getResponse().getStatus() != HttpStatus.SC_NOT_FOUND) {
                // 404 is ok
                throw new IOException(f);
            }
        }
        if (data.getWorkOrder() == null) {
            WorkOrder wo = new WorkOrder();
            wo.setOrderNumber(data.getOrderNumber());
            wo.setRequestDate(LocalDate.now());
            wo.setStatus(new WorkOrderStatus("Requested"));
            wo.setType(new WorkOrderType("Pole Inspection"));
            data.setWorkOrder(wo);
            data.getDomainObjectIsNew().put(wo.getOrderNumber(), true);
        } else {
            data.getDomainObjectIsNew().put(data.getOrderNumber(), false);
        }
        return true;
    }
    
    private static void addNonImageResource(
            WSClientHelper svcs, InspectionData data, FeederInspection finsp, PoleInspection pinsp, ResourceType rtype, File file, String contentType
        ) throws IOException
    {
        ResourceSearchParams params = new ResourceSearchParams();
        if (pinsp != null) {
            params.setAssetInspectionId(pinsp.getId());
        } else {
            params.setSiteInspectionId(finsp.getId());
        }
        params.setType(rtype);
        ResourceMetadata rmeta = CollectionsUtilities.firstItemIn(svcs.resources().search(svcs.token(), params));
        if (rmeta == null) {
            rmeta = new ResourceMetadata();
            if (pinsp != null) {
                rmeta.setAssetId(pinsp.getAssetId());
                rmeta.setAssetInspectionId(pinsp.getId());
                rmeta.setSiteId(pinsp.getSiteId());
                rmeta.setSiteInspectionId(pinsp.getId());
            } else {
                rmeta.setSiteId(finsp.getSiteId());
                rmeta.setSiteInspectionId(finsp.getId());
            }
            rmeta.setContentType(contentType);
            rmeta.setName(file.getName());
            rmeta.setOrderNumber(data.getOrderNumber());
            rmeta.setResourceId(UUID.randomUUID().toString());
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setTimestamp(ZonedDateTime.now());
            rmeta.setType(rtype);
            data.addResourceMetadata(rmeta, file, true);
        } else {
            data.addResourceMetadata(rmeta, file, false);
        }
    }
}
