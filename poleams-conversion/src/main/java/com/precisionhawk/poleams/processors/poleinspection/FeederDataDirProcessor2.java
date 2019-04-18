package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import static com.precisionhawk.poleams.processors.DataImportUtilities.*;
import com.precisionhawk.poleams.processors.FileFilters;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
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
    
    private static final FilenameFilter ANOMALY_MAP_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith("_Map.pdf");
        }
    };
    
    private static final int COL_SEQ = 1; // PH_ID
    private static final int COL_FPLID = 2; // FPL_ID
    private static final int COL_LAT = 3; // Latitude
    private static final int COL_LON = 4; // Longitude
    // Usage
    private static final int COL_LOC_DELTA = 6; // Dist_to_FPL_Loc
//    private static final int COL_TYPE= 10;// Pole_Type

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String POLE_TYPE_NO_POLE = "NO POLE";

    private static final FileFilter DIR_FILTER = FileFilters.DIRECTORY_FILTER;

    private static final FilenameFilter CIRCUIT_MAP_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith("_circuitmap.pdf");
        }
    };
    
    private static final FilenameFilter CSV_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith("_poles_final.csv");
        }
    };
    
    private static final FilenameFilter FEEDER_ANOMALY_RPT_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith("_report.pdf");
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

    private static boolean queueFeederResource(
            WSClientHelper svcs, ProcessListener listener, InspectionData data, File feederDir,
            FilenameFilter fileFilter, ResourceType resourceType, String contentType
       )
    {
        File[] files = feederDir.listFiles(fileFilter);
        switch (files.length) {
            case 0:
                listener.reportMessage(String.format("Unable to locate file for %s", resourceType));
                break;
            case 1:
                try {
                    addNonImageResource(svcs, data, data.getCurrentFeederInspection(), null, ResourceTypes.PoleInspectionAnalysisXML, files[0], contentType);
                } catch (IOException ex) {
                    listener.reportNonFatalException(String.format("Error reading %s for upload as %s", files[0], resourceType), ex);
                }
                break;
            default:
                listener.reportMessage(String.format("Found multiple files for %s", resourceType));
                break;
        }
        
        return true;
    }
    
    // no state
    private FeederDataDirProcessor2() {};

    public static boolean process(Environment env, ProcessListener listener, File feederDir, String orgId, String orderNumber, boolean dryRun) {
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNumber);
        data.setOrganizationId(orgId);
        // find and process CSV files.
        boolean success = true;
        try {
            success = ensureWorkOrder(svcs, data, listener, WorkOrderTypes.DistributionLineInspection);
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
            File byPole = feederDir;
            for (File f : byPole.listFiles(DIR_FILTER)) {
                processPoleDir(svcs, listener, data, f);
            }
        }
        if(dryRun) {
            return true;
        }
        try {
            success = success && saveData(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            success = false;
        }
        //TODO: I think we arun out of memory here.
//        success = success && updateSurveyReport(env, data, listener);
        // Anomaly 
        queueFeederResource(svcs, listener, data, feederDir, ANOMALY_MAP_FILTER, ResourceTypes.FeederAnomalyMap, "application/pdf");
        queueFeederResource(svcs, listener, data, feederDir, CIRCUIT_MAP_FILTER, ResourceTypes.FeederMap, "application/pdf");
        queueFeederResource(svcs, listener, data, feederDir, FEEDER_ANOMALY_RPT_FILTER, ResourceTypes.FeederAnomalyReport, "application/pdf");
        try {
            success = success && saveResources(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            success = false;
        }
        
        return success;
    }

    private static boolean processCSVFile(WSClientHelper svcs, InspectionData data, ProcessListener listener, File f) {
        listener.reportMessage(String.format("Processing CSV file %s", f.getName()));
        Double delta;
        String fplId;
        Reader in = null;
        Float lat;
        Float lon;
        String seq;
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
                    if (data.getCurrentFeeder() == null) {
                        // Parse file name for feeder name and number.
                        String[] parts = f.getName().split("_");
                        String subStationName = parts[0];
                        String feederNum = parts[1];
//                        if ()
//                        parts = parts[3].split("\\.");
//                        String datePart = "20" + parts[0];
//                        inspectionDate = LocalDate.parse(datePart, DATE_FORMAT);
                        inspectionDate = LocalDate.now();
                        if (!ensureFeeder(svcs, data, listener, feederNum, subStationName, "Processed")) return false;
                    }
                    delta = getDouble(listener, record, COL_LOC_DELTA);
                    fplId = record.get(COL_FPLID);
                    if (fplId != null) {
                        fplId = fplId.trim();
                    }
                    seq = record.get(COL_SEQ);
                    if (seq != null) {
                        seq = seq.trim();
                        if (seq.endsWith(".0")) {
                            seq = seq.substring(0, seq.length() - 2);
                        }
                    }
//                    String type = record.get(COL_TYPE);
//                    if (type != null) {
//                        type = type.trim();
//                        if (POLE_TYPE_NO_POLE.equals(type)) {
//                            listener.reportMessage(String.format("Pole %s not found.  Skipping the row %d.", fplId, record.getRecordNumber()));
//                        }
//                    }
                    lat = getFloat(listener, record, COL_LAT);
                    lon = getFloat(listener, record, COL_LON);
                    Pole p = ensurePole(svcs, listener, data, fplId, inspectionDate);
                    PoleInspection insp = ensurePoleInspection(svcs, listener, data, p, inspectionDate);
                    insp.setLatLongDelta(delta);
                    if (p == null) {
                        return false;
                    }
                    if (seq != null && (!seq.isEmpty())) {
                        p.getAttributes().put("Sequence", seq);
                    }
                    if (lat != null && lon != null) {
//                        if ((lat != null && (lat < 25.0 || lat >= 27.0)) || (lon != null && (lon <= -81.0 || lon > -80.0))) {
//                            listener.reportMessage("BAD DATA! Break here!");
//                        }
                        GeoPoint loc;
                        if (p.getLocation() != null) {
                            loc = p.getLocation();
                        } else {
                            loc = new GeoPoint();
                        }
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
        if (!pole.getAttributes().containsKey("Sequence") && seqStr != null && (!seqStr.isEmpty())) {
            pole.getAttributes().put("Sequence", seqStr);
        }
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormats.UNKNOWN.equals(format)) {
                            if (f.getName().endsWith("C.xml")) {
                                // PoleForman XML
                                PoleForemanXMLProcessor.process(listener, pole, insp, f);
                                addNonImageResource(svcs, data, data.getCurrentFeederInspection(), insp, ResourceTypes.PoleInspectionAnalysisXML, f, "application/xml");
                            } else if (f.getName().endsWith("C.pdf")) {
                                // PoleInspectionReport
                                addNonImageResource(svcs, data, data.getCurrentFeederInspection(), insp, ResourceTypes.PoleInspectionReport, f, "application/pdf");
                            } else if (f.getName().endsWith("_DSS.pdf")) {
                                // Drone Survey Sheet
                                addNonImageResource(svcs, data, data.getCurrentFeederInspection(), insp, ResourceTypes.PoleDroneSurveySheet, f, "application/pdf");
                            } else if (f.getName().toLowerCase().endsWith("_ca.pdf")) {
                                // Drone Anomaly Report
                                addNonImageResource(svcs, data, data.getCurrentFeederInspection(), insp, ResourceTypes.PoleAnomalyReport, f, "application/pdf");
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
    
    private static Double getDouble(ProcessListener listener, CSVRecord record, int col) {
        String s = record.get(col);
        try {
            if (s != null) {
                return Double.valueOf(s);
            }
        } catch (NumberFormatException ex) {}
        listener.reportNonFatalError(String.format("Invalid value.  Expecting a numeric value in column %d of row %d", col, record.getRecordNumber()));
        return null;
    }
    
    private static Float getFloat(ProcessListener listener, CSVRecord record, int col) {
        String s = record.get(col);
        try {
            if (s != null) {
                return Float.valueOf(s);
            }
        } catch (NumberFormatException ex) {}
        listener.reportNonFatalError(String.format("Invalid value.  Expecting a numeric value in column %d of row %d", col, record.getRecordNumber()));
        return null;
    }
    
    private static Integer getInteger(ProcessListener listener, CSVRecord record, int col) {
        String s = record.get(col);
        try {
            if (s != null) {
                return Integer.valueOf(s);
            }
        } catch (NumberFormatException ex) {}
        listener.reportNonFatalError(String.format("Invalid value.  Expecting a numeric value in column %d of row %d", col, record.getRecordNumber()));
        return null;
    }

    private static final String SURVEY_REPORT_TEMPLATE = "com/precisionhawk/poleams/processors/poleinspection/Survey_Report_Template.xlsx";
    
    private static boolean updateSurveyReport(Environment env, InspectionData data, ProcessListener listener) {
        try {
            File outFile = File.createTempFile(data.getCurrentFeeder().getFeederNumber(), "surveyrptout");
            InputStream is = null;
            OutputStream os = null;
            try {
                is = FeederDataDirProcessor2.class.getClassLoader().getResourceAsStream(SURVEY_REPORT_TEMPLATE);
                os = new FileOutputStream(outFile);
                org.apache.commons.io.IOUtils.copy(is, os);
            } finally {
                org.apache.commons.io.IOUtils.closeQuietly(is);
                org.apache.commons.io.IOUtils.closeQuietly(os);
            }
            FeederInspectionSummary summary = env.obtainWebService(FeederInspectionWebService.class).retrieveSummary(env.obtainAccessToken(), data.getCurrentFeederInspection().getId());
            boolean success = SurveyReportGenerator.populateTemplate(env, listener, summary, outFile, outFile, true);
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
