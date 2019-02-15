package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.AssetSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

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
            return name.toUpperCase().contains("RGB");
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
        for (File f : feederDir.listFiles(CSV_FILE_FILTER)) {
            success = success && processCSVFile(svcs, data, listener, f);
        }
        if (!success) {
            return true;
        }
        // find and process pole dirs.
        for (File f : feederDir.listFiles(DIR_FILTER)) {
            processImagesDir(svcs, listener, data, f);
        }
        return true;
    }

    private static boolean processCSVFile(WSClientHelper svcs, InspectionData data, ProcessListener listener, File f) {
        String fplId;
        Reader in = null;
        Float lat;
        Float lon;
        String s;
        try {
            in = new FileReader(f);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                if (record.getRecordNumber() == 0) {
                    if (record.size() < 10) {
                        listener.reportMessage("The file \"%s\" has too few columns.  It is being skipped.");
                        return true;
                    }
                } else {
                    if (data.getFeeder() == null) {
                        // Parse file name for feeder name and number.
                        String[] parts = f.getName().split("_");
                        String subStationName = parts[0];
                        String feederNum = parts[1];
                        ensureFeeder(svcs, data, listener, feederNum, subStationName);
                    }
                    fplId = record.get(COL_FPLID);
                    lat = getFloat(listener, record, COL_LAT);
                    lon = getFloat(listener, record, COL_LON);
                    Pole p = ensurePole(svcs, listener, data, fplId);
                    GeoPoint loc = new GeoPoint();
                    loc.setLatitude(lat.doubleValue());
                    loc.setLongitude(lon.doubleValue());
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }

    private static boolean processImagesDir(WSClientHelper svcs, ProcessListener listener, InspectionData data, File dir) {
        // Parse sequence and FPLID from file name.
        String[] parts = dir.getName().split("_");
        String seqStr = parts[0];
        String fplid = parts.length > 1 ? parts[1] : null;
        if (fplid == null) {
            //TODO: What to do here?
            return true;
        }
        Pole pole = ensurePole(svcs, listener, data, fplid);
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormat.IMAGE_FORMAT_UNKNOWN.equals(format)) {
                            listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", f));
                        } else {
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

    private static void ensureFeeder(WSClientHelper svcs, InspectionData data, ProcessListener listener, String feederNum, String subStationName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static Pole ensurePole(WSClientHelper svcs, ProcessListener listener, InspectionData data, String fplid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
