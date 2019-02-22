package com.precisionhawk.poleams.processors.translineinspection;

import com.precisionhawk.poleams.processors.MasterDataImporter;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.domain.WorkOrderStatuses;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.FilenameFilters;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.ClientResponseFailure;
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
public class ShapeFileMasterDataImport implements MasterDataImporter {
    
    private WSClientHelper svcs;
    private final InspectionData data = new InspectionData();
    private ProcessListener listener;
    
    private static final ZoneId DEFAULT_TZ = ZoneId.of("America/New_York");

    @Override
    public boolean process(Environment env, ProcessListener listener, File poleDataShapeFile, String orderNum, String organizationId) {
        this.listener = listener;
        svcs = new WSClientHelper(env);
        InputStream is = null;
        data.setOrganizationId(organizationId);
        data.setCurrentOrderNumber(orderNum);
        try {            
            // Parse the shape file (KML)
            ShapeFileDocumentHandler handler = new ShapeFileDocumentHandler();
            is = new BufferedInputStream(new FileInputStream(poleDataShapeFile));
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));
            
            // Find the directory which contains images, if any
            File imagesDir = new File(poleDataShapeFile.getParentFile(), "Data Analytics");
            if (imagesDir.isDirectory()) {
                File[] files = imagesDir.listFiles(FilenameFilters.IMAGES_FILTER);
                    for (File imageFile : files) {
                        if (imageFile.isFile()) {
                            if (imageFile.canRead()) {
                                try {
                                    ImageFormat format = Imaging.guessFormat(imageFile);
                                    if (ImageFormat.IMAGE_FORMAT_UNKNOWN.equals(format)) {
                                        listener.reportNonFatalError(String.format("Unexpected file \"%s\" is being skipped.", imageFile));
                                    } else {
                                        processImageFile(svcs, listener, data, imageFile, format);
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
            }
            
            boolean success = true;
            
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
    
    private TransmissionLine ensureTransLine(String lineNum) {
        try {
            // Line
            TransmissionLineSearchParams params = new TransmissionLineSearchParams();
            params.setLineNumber(lineNum);
            params.setOrganizationId(data.getOrganizationId());
            data.setCurrentLine(CollectionsUtilities.firstItemIn(svcs.transmissionLines().search(svcs.token(), params)));
            if (data.getCurrentLine() == null) {
                TransmissionLine line = new TransmissionLine();
                line.setLineNumber(lineNum);
                line.setId(UUID.randomUUID().toString());
                line.setName(lineNum);
                line.setOrganizationId(data.getOrganizationId());
                data.setCurrentLine(line);
                data.getDomainObjectIsNew().put(line.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getCurrentLine().getId(), false);
            }

            // Now that we have the line, we can deal with the Work Order
            // Work Order
            try {
                data.setCurrentWorkOrder(svcs.workOrders().retrieveById(svcs.token(), data.getCurrentOrderNumber()));
            } catch (ClientResponseFailure ex) {
                if (ex.getResponse().getResponseStatus() == Status.NOT_FOUND) {
                    data.setCurrentWorkOrder(null);
                } else {
                    throw ex;
                }
            }
            if (data.getCurrentWorkOrder() == null) {
                WorkOrder wo = new WorkOrder();
                wo.setOrderNumber(data.getCurrentOrderNumber());
                wo.setRequestDate(LocalDate.now());
                wo.setStatus(WorkOrderStatuses.Requested);
                wo.setType(WorkOrderTypes.DistributionLineInspection);
                data.setCurrentWorkOrder(wo);
                data.getDomainObjectIsNew().put(wo.getOrderNumber(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getCurrentOrderNumber(), false);
            }
            boolean found = false;
            for (String siteId : data.getCurrentWorkOrder().getSiteIds()) {
                if (data.getCurrentLine().getId().equals(siteId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                data.getCurrentWorkOrder().getSiteIds().add(data.getCurrentLine().getId());
            }

            // Now with line and work order, we can deal with the line inspection
            // Line Inspection
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setOrderNumber(data.getCurrentOrderNumber());
            siparams.setSiteId(data.getCurrentLine().getId());
            data.setCurrentLineInspection(CollectionsUtilities.firstItemIn(svcs.transmissionLineInspections().search(svcs.token(), siparams)));
            if (data.getCurrentLineInspection() == null) {
                TransmissionLineInspection insp = new TransmissionLineInspection();
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getCurrentOrderNumber());
                insp.setSiteId(data.getCurrentLine().getId());
                insp.setStatus(new SiteInspectionStatus("Pending")); //FIXME:
                insp.setType(new SiteInspectionType("DroneInspection")); //FIXME:
                data.setCurrentLineInspection(insp);
                data.getDomainObjectIsNew().put(insp.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getCurrentLineInspection().getId(), false);
            }
            return data.getCurrentLine();
        } catch (IOException ioe) {
            listener.reportFatalException(ioe);
            return null;
        }
    }
    
    private TransmissionStructure ensureTransStruct(String structureNum, GeoPoint location) {
        if (structureNum == null || structureNum.isEmpty()) {
            listener.reportFatalError("Utility ID missing");
            return null;
        }
        try {
            // Structure
            TransmissionStructureSearchParams  pparams = new TransmissionStructureSearchParams();
            pparams.setSiteId(data.getCurrentLine().getId());
            pparams.setStructureNumber(structureNum);
            TransmissionStructure struct = CollectionsUtilities.firstItemIn(svcs.transmissionStructures().search(svcs.token(), pparams));
            if (struct == null) {
                struct = new TransmissionStructure();
                struct.setId(UUID.randomUUID().toString());
                struct.setLocation(location);
                struct.setName(structureNum);
                struct.setSiteId(data.getCurrentLine().getId());
                struct.setStructureNumber(structureNum);
                data.addTransmissionStruture(struct, true);
            } else {
                data.addTransmissionStruture(struct, false);
            }
            
            // Pole Inspection
            AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
            aiparams.setAssetId(struct.getId());
            aiparams.setOrderNumber(data.getCurrentOrderNumber());
            TransmissionStructureInspection insp = CollectionsUtilities.firstItemIn(svcs.transmissionStructureInspections().search(svcs.token(), aiparams));
            if (insp == null) {
                insp = new TransmissionStructureInspection();
                insp.setAssetId(struct.getId());
                insp.setOrderNumber(data.getCurrentOrderNumber());
                insp.setSiteId(data.getCurrentLine().getId());
                insp.setSiteInspectionId(data.getCurrentLineInspection().getId());
                insp.setStatus(new AssetInspectionStatus("Pending")); //FIXME:
                insp.setType(new AssetInspectionType("DroneInspection")); //FIXME:
                data.addTransmissionStructureInspection(struct, insp, true);
            } else {
                data.addTransmissionStructureInspection(struct, insp, false);
            }
            
            return struct;
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            return null;
        }
    }

    private void processImageFile(WSClientHelper wsclient, ProcessListener listener, InspectionData data, File f, ImageFormat format)
        throws IOException, ImageReadException
    {
        listener.reportMessage(String.format("Processing image file %s", f.getAbsolutePath()));
        
        // the name should be something like 72_Chip middle left phase.jpg where 72 is the structure number
        String[] parts = f.getName().split("_");
        if (parts.length < 2) {
            listener.reportNonFatalError(String.format("Invalid image name %s", f.getName()));
            return;
        }
        TransmissionStructure struct = data.getStructuresMap().get(new SiteAssetKey(data.getCurrentLine().getId() ,parts[0]));
        if (struct == null) {
            listener.reportNonFatalError(String.format("Unable to locate structure %s for image %s", parts[0], f.getName()));
            return;
        }
        
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
            IImageMetadata metadata = Imaging.getMetadata(f);
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
//            String posSide = resourcePositionSide(listener, f);
//            if (posSide != null) {
//                ImagePosition pos = new ImagePosition();
//                pos.setSide(posSide);
//                rmeta.setPosition(pos);
//            }
            rmeta.setAssetId(struct.getId());
            rmeta.setAssetInspectionId(data.getStructureInspectionsMap().get(new SiteAssetKey(struct)).getId());
            rmeta.setSize(ImageUtilities.getSize(info));
            rmeta.setStatus(ResourceStatus.QueuedForUpload);
            rmeta.setSiteId(data.getCurrentLine().getId());
            rmeta.setSiteInspectionId(data.getCurrentLineInspection().getId());
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
    
    private static final String TAG_COORDS = "coordinates";
    private static final String TAG_FOLDER = "Folder";
    private static final String TAG_NAME = "name";
    private static final String TAG_PLACEMARK = "Placemark";
    
    class ShapeFileDocumentHandler extends AbstractDocumentHandler {
        private TransmissionLine currentLine;
        private boolean inFolder = false;
        private GeoPoint poleLocation;
        private String utilityId;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case TAG_FOLDER:
                    // Start tag for line data
                    assertLineNotExists();
                    inFolder = true;
                    break;
                case TAG_PLACEMARK:
                    // Start tag for structure data
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
                    // Coordinates for structure
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
                case TAG_FOLDER:
                    // End tag for line data
                    assertLineExists();
                    inFolder = false;
                    break;
                case TAG_NAME:
                    if (inFolder) {
                        // Name for either line or structure
                        if (currentLine == null) {
                            // Assume this is a line
                            currentLine = ensureTransLine(super.textbuffer.toString().trim());
                            assertLineExists();
                        } else {
                            // Assume this is a structure
                            // Structure name looks something like 1001/122 where 1001 is line ID
                            s = super.textbuffer.toString().trim();
                            int i = s.indexOf("/");
                            if (i > -1) {
                                s = s.substring(++i);
                            }
                            utilityId = s;
                        }
                    }
                    break;
                case TAG_PLACEMARK:
                    // End tag for pole data
                    TransmissionStructure p = ensureTransStruct(utilityId, poleLocation);
                    if (p == null) {
                        throw new SAXException(String.format("Unable to create new trans. structure %s", utilityId));
                    }
                    poleLocation = null;
                    utilityId = null;
                    break;
                default:
                    // Not handled
            }
        }
        
        private void assertLineExists() throws SAXException {
            if (currentLine == null) {
                throw new SAXException("Trans. line is expected, but does not exist.");
            }
        }
        
        private void assertLineNotExists() throws SAXException {
            if (currentLine != null) {
                throw new SAXException("Trans. line is not expected, but does exist.");
            }
        }
    }
}
