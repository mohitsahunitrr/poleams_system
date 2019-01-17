package com.precisionhawk.poleams.processors.poleinspection;

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
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
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
import javax.ws.rs.core.Response;
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
    
    private Feeder ensureFeeder(String feederId) {
        try {
            // Feeder
            FeederSearchParams params = new FeederSearchParams();
            params.setFeederNumber(feederId);
            params.setOrganizationId(data.getOrganizationId());
            data.setFeeder(CollectionsUtilities.firstItemIn(svcs.feeders().search(svcs.token(), params)));
            if (data.getFeeder() == null) {
                Feeder feeder = new Feeder();
                feeder.setFeederNumber(feederId);
                feeder.setId(UUID.randomUUID().toString());
                feeder.setName(feederId);
                feeder.setOrganizationId(data.getOrganizationId());
                data.setFeeder(feeder);
                data.getDomainObjectIsNew().put(feeder.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getFeeder().getId(), false);
            }

            // Now that we have the feeder, we can deal with the Work Order
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
                if (data.getFeeder().getId().equals(siteId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                data.getWorkOrder().getSiteIds().add(data.getFeeder().getId());
            }

            // Now with feeder and work order, we can deal with the feeder inspection
            // Feeder Inspection
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setOrderNumber(data.getOrderNumber());
            siparams.setSiteId(data.getFeeder().getId());
            data.setFeederInspection(CollectionsUtilities.firstItemIn(svcs.feederInspections().search(svcs.token(), siparams)));
            if (data.getFeederInspection() == null) {
                FeederInspection insp = new FeederInspection();
                insp.setId(UUID.randomUUID().toString());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getFeeder().getId());
                insp.setStatus(new SiteInspectionStatus("Pending")); //FIXME:
                insp.setType(new SiteInspectionType("DroneInspection")); //FIXME:
                data.setFeederInspection(insp);
                data.getDomainObjectIsNew().put(insp.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getFeederInspection().getId(), false);
            }
            return data.getFeeder();
        } catch (IOException ioe) {
            listener.reportFatalException(ioe);
            return null;
        }
    }
    
    private Pole ensurePole(String utilityId, GeoPoint location) {
        if (utilityId == null || utilityId.isEmpty()) {
            listener.reportFatalError("Utility ID missing");
            return null;
        }
        try {
            // Pole
            PoleSearchParams pparams = new PoleSearchParams();
            pparams.setSiteId(data.getFeeder().getId());
            pparams.setUtilityId(utilityId);
            Pole pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
            if (pole == null) {
                pole = new Pole();
                pole.setId(UUID.randomUUID().toString());
                pole.setLocation(location);
                pole.setName(utilityId);
                pole.setSiteId(data.getFeeder().getId());
                pole.setUtilityId(utilityId);
                data.addPole(pole, true);
            } else {
                data.addPole(pole, false);
            }
            
            // Pole Inspection
            AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
            aiparams.setAssetId(pole.getId());
            aiparams.setOrderNumber(data.getOrderNumber());
            PoleInspection insp = CollectionsUtilities.firstItemIn(svcs.poleInspections().search(svcs.token(), aiparams));
            if (insp == null) {
                insp = new PoleInspection();
                insp.setAssetId(pole.getId());
                insp.setOrderNumber(data.getOrderNumber());
                insp.setSiteId(data.getFeeder().getId());
                insp.setSiteInspectionId(data.getFeederInspection().getId());
                insp.setStatus(new AssetInspectionStatus("Pending")); //FIXME:
                insp.setType(new AssetInspectionType("DroneInspection")); //FIXME:
                data.addPoleInspection(pole, insp, true);
            } else {
                data.addPoleInspection(pole, insp, false);
            }
            
            return pole;
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
        private Feeder currentFeeder;
        private boolean inFolder = false;
        private GeoPoint poleLocation;
        private String utilityId;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName) {
                case TAG_FOLDER:
                    // Start tag for feeder data
                    assertFeederNotExists();
                    inFolder = true;
                    break;
                case TAG_PLACEMARK:
                    // Start tag for pole data
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
                case TAG_FOLDER:
                    // End tag for feeder data
                    assertFeederExists();
                    inFolder = false;
                    break;
                case TAG_NAME:
                    if (inFolder) {
                        // Name for either feeder or pole
                        if (currentFeeder == null) {
                            // Assume this is a feeder
                            currentFeeder = ensureFeeder(super.textbuffer.toString().trim());
                            assertFeederExists();
                        } else {
                            // Assume this is a pole
                            // Pole name looks something like 1001/122 where 1001 is feeder ID
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
                    Pole p = ensurePole(utilityId, poleLocation);
                    if (p == null) {
                        throw new SAXException(String.format("Unable to create new pole %s", utilityId));
                    }
                    poleLocation = null;
                    utilityId = null;
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
