package com.precisionhawk.poleams.processors.translineinspection;

import com.precisionhawk.poleams.processors.MasterDataImporter;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionStatus;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.domain.WorkOrderStatuses;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
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
    
    @Override
    public boolean process(Environment env, ProcessListener listener, File poleDataShapeFile, String orderNum, String organizationId) {
        this.listener = listener;
        svcs = new WSClientHelper(env);
        InputStream is = null;
        data.setOrganizationId(organizationId);
        data.setOrderNumber(orderNum);
        try {            
            // Parse the shape file (KML)
            ShapeFileDocumentHandler handler = new ShapeFileDocumentHandler();
            is = new BufferedInputStream(new FileInputStream(poleDataShapeFile));
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));
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
            data.setLine(CollectionsUtilities.firstItemIn(svcs.transmissionLines().search(svcs.token(), params)));
            if (data.getLine() == null) {
                TransmissionLine line = new TransmissionLine();
                line.setLineNumber(lineNum);
                line.setId(UUID.randomUUID().toString());
                line.setName(lineNum);
                line.setOrganizationId(data.getOrganizationId());
                data.setLine(line);
                data.getDomainObjectIsNew().put(line.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getLine().getId(), false);
            }

            // Now that we have the line, we can deal with the Work Order
            // Work Order
            try {
                data.setWorkOrder(svcs.workOrders().retrieveById(svcs.token(), data.getOrderNumber()));
            } catch (ClientResponseFailure ex) {
                if (ex.getResponse().getResponseStatus() == Status.NOT_FOUND) {
                    data.setWorkOrder(null);
                } else {
                    throw ex;
                }
            }
            if (data.getWorkOrder() == null) {
                WorkOrder wo = new WorkOrder();
                wo.setOrderNumber(data.getOrderNumber());
                wo.setRequestDate(LocalDate.now());
                wo.setStatus(WorkOrderStatuses.Requested);
                wo.setType(WorkOrderTypes.DistributionLineInspection);
                data.setWorkOrder(wo);
                data.getDomainObjectIsNew().put(wo.getOrderNumber(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getOrderNumber(), false);
            }
            boolean found = false;
            for (String siteId : data.getWorkOrder().getSiteIds()) {
                if (data.getLine().getId().equals(siteId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                data.getWorkOrder().getSiteIds().add(data.getLine().getId());
            }

            // Now with line and work order, we can deal with the line inspection
            // Line Inspection
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setOrderNumber(data.getOrderNumber());
            siparams.setSiteId(data.getLine().getId());
            data.setLineInspection(CollectionsUtilities.firstItemIn(svcs.transmissionLineInspections().search(svcs.token(), siparams)));
            if (data.getLineInspection() == null) {
                TransmissionLineInspection insp = new TransmissionLineInspection();
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getLine().getId());
                insp.setStatus(new SiteInspectionStatus("Pending")); //FIXME:
                insp.setType(new SiteInspectionType("DroneInspection")); //FIXME:
                data.setLineInspection(insp);
                data.getDomainObjectIsNew().put(insp.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getLineInspection().getId(), false);
            }
            return data.getLine();
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
            pparams.setSiteId(data.getLine().getId());
            pparams.setStructureNumber(structureNum);
            TransmissionStructure struct = CollectionsUtilities.firstItemIn(svcs.transmissionStructures().search(svcs.token(), pparams));
            if (struct == null) {
                struct = new TransmissionStructure();
                struct.setId(UUID.randomUUID().toString());
                struct.setLocation(location);
                struct.setName(structureNum);
                struct.setSiteId(data.getLine().getId());
                struct.setStructureNumber(structureNum);
                data.addTransmissionStruture(struct, true);
            } else {
                data.addTransmissionStruture(struct, false);
            }
            
            // Pole Inspection
            AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
            aiparams.setAssetId(struct.getId());
            aiparams.setOrderNumber(data.getOrderNumber());
            TransmissionStructureInspection insp = CollectionsUtilities.firstItemIn(svcs.transmissionStructureInspections().search(svcs.token(), aiparams));
            if (insp == null) {
                insp = new TransmissionStructureInspection();
                insp.setAssetId(struct.getId());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getLine().getId());
                insp.setSiteInspectionId(data.getLineInspection().getId());
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
