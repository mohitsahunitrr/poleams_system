package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.IOUtils;
import org.papernapkin.liana.xml.sax.AbstractDocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author pchapman
 */
public class PPLInspectionDataImport {
    
    private WSClientHelper svcs;
    private final InspectionData data = new InspectionData();
    private ProcessListener listener;
    
    class FeederDirFileFilter implements FilenameFilter {
        Map<String, Feeder> feedersByFeederNum;
        FeederDirFileFilter(Map<String, Feeder> feedersByFeederNum) {
            this.feedersByFeederNum = feedersByFeederNum;
        }
        @Override
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return f.isDirectory() && feedersByFeederNum.containsKey(name);
        }
    };
    private final FilenameFilter DRONE_IMAGE_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.contains("rgb") && name.endsWith(".jpg");
        }
    };
    private final FilenameFilter KML_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".kml");
        }
    };
    private final FilenameFilter MANUAL_IMAGE_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return false;
        }
    };
    private final FilenameFilter POLE_IMAGES_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            return f.isDirectory() && name.contains("_Pole");
        }
    };
    private final FilenameFilter THERM_IMAGE_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.contains("thermal") && name.endsWith(".jpg");
        }
    };
    
    public boolean process(Environment env, ProcessListener listener, String orderNum, String organizationId, File importDir) {
        this.listener = listener;
        svcs = new WSClientHelper(env);
        InputStream is = null;
        data.setOrganizationId(organizationId);
        data.setCurrentOrderNumber(orderNum);
        try {
            boolean success = DataImportUtilities.ensureWorkOrder(svcs, data, listener, WorkOrderTypes.DistributionLineInspection);
            if (!success) {
                return success;
            }
            
            // Parse the shape file (KML)
            File[] poleDataShapeFiles = importDir.listFiles(KML_FILE_FILTER);
            if (poleDataShapeFiles.length == 0) {
                listener.reportFatalError(String.format("No KML files found in %s", importDir.getAbsolutePath()));
                return false;
            } else if (poleDataShapeFiles.length > 1) {
                listener.reportFatalError(String.format("Multiple KML files found in %s", importDir.getAbsolutePath()));
                return false;
            }
            ShapeFileDocumentHandler handler = new ShapeFileDocumentHandler(svcs, listener);
            is = new BufferedInputStream(new FileInputStream(poleDataShapeFiles[0]));
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));
            
            // Locate and process images for the poles
            File polesDir;
            ImagesProcessor imagesProcessor = new ImagesProcessor();
            imagesProcessor.setDroneImageFilter(DRONE_IMAGE_FILE_FILTER);
            imagesProcessor.setManualImageFilter(MANUAL_IMAGE_FILE_FILTER);
            imagesProcessor.setThermalImageFilter(THERM_IMAGE_FILE_FILTER);
            for (File feederDir : importDir.listFiles(new FeederDirFileFilter(data.getFeedersByFeederNum()))) {
                DataImportUtilities.ensureFeeder(svcs, data, listener, feederDir.getName(), feederDir.getName());
                polesDir = new File(feederDir, "Poles");
                if (polesDir.isDirectory()) {
                    // Process each directory which should be associated with a pole with name {feedername}_Pole{polenumber}
                    for (File imagesDir : polesDir.listFiles(POLE_IMAGES_FILE_FILTER)) {
                        String poleNumStr = imagesDir.getName().split("_")[1].replace("Pole", "");
                        Integer poleNum = Integer.valueOf(poleNumStr); // an alternative could be to remove left padded zeros.
                        Pole p = DataImportUtilities.ensurePole(svcs, listener, data, poleNum.toString(), LocalDate.now());
                        for (File f : imagesDir.listFiles()) {
                            if (f.isFile()) {
                                try {
                                    ImageFormat format = Imaging.guessFormat(f);
                                    if (ImageFormat.IMAGE_FORMAT_UNKNOWN.equals(format)) {
                                        listener.reportMessage(String.format("Skipping unexpected file %s", f.getName()));
                                    } else {
                                        imagesProcessor.process(env, listener, data, p, f, format);
                                    }
                                } catch (ImageReadException ex) {
                                    listener.reportNonFatalError(String.format("Unable to determine type or read the metadata of file %s, it will be skipped", f.getName()));
                                }
                            }
                        }
                    }
                } else {
                    listener.reportMessage(String.format("Unable to find valid \"Poles\" directory in %s", feederDir.getAbsolutePath()));
                }
            }
            
            if (success) {
                listener.reportMessage("Saving data...");
                success = DataImportUtilities.saveData(svcs, listener, data);
            }
            
            if (success) {
                listener.reportMessage("Saving resources...");
                success = DataImportUtilities.saveResources(svcs, listener, data);
            }
            return success;
        } catch (IOException | SAXException ex) {
            listener.reportFatalException(ex);
            return false;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    private Pole ensurePole(String utilityId, GeoPoint location) throws IOException {
        if (utilityId == null || utilityId.isEmpty()) {
            listener.reportFatalError("Utility ID missing");
            return null;
        }
        Pole pole = DataImportUtilities.ensurePole(svcs, listener, data, utilityId, LocalDate.now());
        if (pole == null) {
            throw new IOException(String.format("Unable to lookup or create the pole %s", utilityId));
        }
        if (pole.getLocation() == null) {
            pole.setLocation(location);
        } else {
            pole.getLocation().setAccuracy(location.getAccuracy());
            pole.getLocation().setAltitude(location.getAltitude());
            pole.getLocation().setLatitude(location.getLatitude());
            pole.getLocation().setLongitude(location.getLongitude());
        }
        return pole;
    }
    
    private static final String TAG_COORDS = "coordinates";
    private static final String TAG_DESC = "description";
    private static final String TAG_FOLDER = "Folder";
    private static final String TAG_NAME = "name";
    private static final String TAG_PLACEMARK = "Placemark";
    
    class ShapeFileDocumentHandler extends AbstractDocumentHandler {
        private Feeder currentFeeder;
        private String feederName;
        private boolean inFolder = false;
        private boolean inPlacemark = false;
        private final ProcessListener listener;
        private GeoPoint poleLocation;
        private final WSClientHelper svcs;
        private String utilityId;
        
        ShapeFileDocumentHandler(WSClientHelper svcs, ProcessListener listener) {
            this.listener = listener;
            this.svcs = svcs;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case TAG_DESC:
                    // Not Handled
                    break;
                case TAG_FOLDER:
                    // Start tag for feeder data
                    assertFeederNotExists();
                    inFolder = true;
                    break;
                case TAG_PLACEMARK:
                    // Start tag for pole data
                    inPlacemark = true;
                    poleLocation = null;
                    utilityId = null;
                default:
                    // Not handled
            }
        }
        
        @Override
        protected void _endElement(String uri, String localName, String qName) throws SAXException {
            String s;
            switch (qName) {
                case TAG_COORDS:
                    // Coordinates for pole
                    s = super.textbuffer.toString().trim();
                    String[] coords = s.split(",");
                    if (coords.length < 2 || coords.length > 3) {
                        throw new SAXException(String.format("Unexpected coordinates value %s", s));
                    }
                    poleLocation = new GeoPoint();
                    try {
                        poleLocation.setLongitude(Double.valueOf(coords[0]));
                        poleLocation.setLatitude(Double.valueOf(coords[1]));
                        if (coords.length == 3) {
                            poleLocation.setAltitude(Double.valueOf(coords[2]));
                        }
                    } catch (NumberFormatException ex) {
                        throw new SAXException(String.format("Unexpected coordinates value %s", s), ex);
                    }
                    break;
                case TAG_DESC:
                    if (inPlacemark) {
                        feederName = super.textbuffer.toString().trim();
                        try {
                            DataImportUtilities.ensureFeeder(svcs, data, listener, feederName, feederName);
                            currentFeeder = data.getCurrentFeeder();
                        } catch (IOException ex) {
                            throw new SAXException(ex);
                        }
                    } break;
                case TAG_FOLDER:
                    // End tag for feeder data
                    assertFeederExists();
                    inFolder = false;
                    break;
                case TAG_NAME:
                    if (inPlacemark) {
                        // Name of pole
                        utilityId = super.textbuffer.toString().trim();
                    }
                    break;
                case TAG_PLACEMARK:
                    // End tag for pole data
                    try {
                        Pole p = ensurePole(utilityId, poleLocation);
                        if (p == null) {
                            throw new SAXException(String.format("Unable to create new pole %s", utilityId));
                        }
                        inPlacemark = false;
                        poleLocation = null;
                        utilityId = null;
                    } catch (IOException ex) {
                        throw new SAXException(ex);
                    }
                    break;
                default:
                    // Not handled
            }
        }
        
        private void assertFeederExists() throws SAXException {
            if (currentFeeder == null) {
                throw new SAXException("Feeder is expected, but does not exist.");
            }
        }
        
        private void assertFeederNotExists() throws SAXException {
            if (currentFeeder != null) {
                throw new SAXException("Feeder is not expected, but does exist.");
            }
        }
    }
}
