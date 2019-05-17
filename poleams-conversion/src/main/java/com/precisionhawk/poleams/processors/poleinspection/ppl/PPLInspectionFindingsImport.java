package com.precisionhawk.poleams.processors.poleinspection.ppl;

import com.precisionhawk.ams.bean.InspectionEventResourceSearchParams;
import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.domain.InspectionEventPolygon;
import com.precisionhawk.ams.domain.InspectionEventResource;
import com.precisionhawk.ams.domain.InspectionEventSource;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.InspectionEvent;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.FileFilters;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.RegexFilenameFilter;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.papernapkin.liana.util.StringUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author pchapman
 */
public class PPLInspectionFindingsImport implements PPLConstants {
    
    private static final int COL_POLE_NUM = 0;
    private static final int COL_IMAGE_NUM = 33;
    private static final int COL_ANOMOLY_TYPE = 34;
    private static final int COL_ANOMOLY_DESC = 35;
    
    private PPLInspectionFindingsImport() {}
    
    public static void process(Environment env, ProcessListener listener, File dataDir, String feederId, String orderNum, boolean dryRun) {
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        
        data.setOrganizationId(ORG_ID);
        data.setCurrentOrderNumber(orderNum);
        try {
            boolean success = DataImportUtilities.ensureWorkOrder(svcs, data, listener, WorkOrderTypes.DistributionLineInspection);
            if (!success) {
                listener.reportFatalError(String.format("Unable to look up order %s", orderNum));
                return;
            }
            
            try {
                data.setCurrentFeeder(svcs.feeders().retrieve(svcs.token(), feederId));
            } catch (NotFoundException nfe) {
                data.setCurrentFeeder(null);
            }
            if (data.getCurrentFeeder() == null) {
                listener.reportFatalError(String.format("Unable to look up feeder %s", feederId));
                return;
            }
            data.addFeeder(data.getCurrentFeeder(), false);
            
            success = DataImportUtilities.ensureFeederInspection(svcs, data, listener, data.getCurrentFeeder(), SI_PROCESSED);
            if (!success) {
                listener.reportFatalError("Unable to look up or create feeder inspection");
                return;
            }
            
            // Find and process the CSV file
            File[] csvFiles = dataDir.listFiles(FileFilters.CSV_FILTER);
            switch (csvFiles.length) {
                case 0:
                    listener.reportFatalError("Inventory CSV file not found");
                    return;
                case 1:
                    // Parse
                    if (!processCSVFile(svcs, listener, data, csvFiles[0])) {
                        return;
                    }
                    break;
                default:
                    listener.reportFatalError(String.format("%d inventory CSV files found", csvFiles.length));
                    return;
            }
            
            if (!dryRun) {
                DataImportUtilities.saveData(svcs, listener, data);
            }
        } catch (IOException | SAXException ex) {
            listener.reportFatalException(ex);
        }
    }

    private static boolean processCSVFile(WSClientHelper svcs, ProcessListener listener, InspectionData data, File csvFile) throws IOException, SAXException {
        String anomalyDesc;
        String anomalyType;
        String imageNum;
        Reader in = null;
        PoleInspection insp = null;
        Pole pole = null;
        String poleNum;

        try {
            in = new FileReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                // First two rows are header and empty
                anomalyDesc = record.get(COL_ANOMOLY_DESC);
                anomalyType = record.get(COL_ANOMOLY_TYPE);
                imageNum = record.get(COL_IMAGE_NUM);
                poleNum = record.get(COL_POLE_NUM);
                
                if (anomalyType != null && !anomalyType.isEmpty()) {
                    if (StringUtil.isNumeric(poleNum)) {
                        listener.reportMessage(String.format("Processing pole %s", poleNum));
                        pole = DataImportUtilities.ensurePole(svcs, listener, data, poleNum, LocalDate.now());
                        if (pole == null) {
                            listener.reportFatalError(String.format("Unable to locate or create pole %s", poleNum));
                            return false;
                        }
                        insp = data.getPoleInspectionsMap().get(new SiteAssetKey(pole));
                        if (insp == null) {
                            listener.reportFatalError(String.format("Unable to locate inspection for pole %s", poleNum));
                            return false;
                        }
                    } else if ((poleNum == null || "".equals(poleNum)) && pole != null) {
                        // Another anomaly/image for previous pole
                    } else {
                        listener.reportNonFatalError(String.format("Value \"%s\" is not a number, which is required for a pole number.  Row %d will be skipped.", poleNum, record.getRecordNumber()));
                        continue;
                    }
                    String[] descs = anomalyDesc.split(";");
                    String[] types = anomalyType.split(";");
                    if (descs.length != types.length) {
                        listener.reportNonFatalError(String.format("The number of anomoly descriptions do not match the number of anomoly types in row %d.", record.getRecordNumber()));
                    } else {
                        for (int i = 0; i < types.length; i++) {
                            if (!reportAnamoly(svcs, listener, data, csvFile.getParentFile(), pole, insp, types[i].trim(), descs[i].trim(), imageNum)) {
                                return false;
                            }
                        }
                    }
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // Do nothing
                }
            }
        }
        return true;
    }

    private static boolean reportAnamoly(
            WSClientHelper svcs, ProcessListener listener, InspectionData data, File parentFile, Pole pole,
            PoleInspection insp, String anomalyType, String anomalyDesc, String imageNum
        )
        throws IOException, SAXException
    {
        InspectionEventSearchParams ieparams = new InspectionEventSearchParams();
        ieparams.setAssetId(pole.getId());
        ieparams.setOrderNumber(data.getCurrentOrderNumber());
        InspectionEvent evt = null;
        for (InspectionEvent ie : svcs.inspectionEvents().search(svcs.token(), ieparams)) {
            if (ie.getFindingType().equals(anomalyType)) {
                evt = ie;
                break;
            }
        }
        if (evt == null) {
            evt = new InspectionEvent();
            evt.setAssetId(pole.getId());
            evt.setComment(anomalyDesc);
            evt.setDate(LocalDate.now());
            evt.setFindingType(anomalyType);
            evt.setId(UUID.randomUUID().toString());
            evt.setName(String.format("Pole %s - %s", pole.getUtilityId(), anomalyType));
            evt.setOrderNumber(data.getCurrentOrderNumber());
            evt.setSeverity(3);
            evt.setSiteId(pole.getSiteId());
            evt.setSource(InspectionEventSource.AI);
            data.addInspectionEvent(evt, true);
        } else {
            data.addInspectionEvent(evt, false);
        }
        
        // There *may* be a related XML
        List<InspectionEventPolygon> polys = new LinkedList();
        File[] xmls = parentFile.listFiles(new RegexFilenameFilter(RegexBuilder.xmlFileNameRegex(pole.getUtilityId(), imageNum)));
        if (xmls.length == 1) {
            InputStream is = null;
            try {
                polys = parseForPolygons(evt, new FileInputStream(xmls[0]));
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        
        String imagesRegex;
        if (polys == null || polys.isEmpty()) {
            // Thermal
            imagesRegex = RegexBuilder.imageFileNameRegex(pole.getUtilityId(), imageNum);
        } else {
            imagesRegex = RegexBuilder.rgbImageFileNameRegex(pole.getUtilityId(), imageNum);
        }
        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setAssetId(pole.getId());
        rparams.setOrderNumber(data.getCurrentOrderNumber());
        List<ResourceMetadata> imgList = new LinkedList();
        for (ResourceMetadata r : svcs.resources().search(svcs.token(), rparams)) {
            if (r.getSourceResourceId() == null && r.getName() != null && r.getName().matches(imagesRegex)) {
                imgList.add(r);
            }
        }
        if (imgList.isEmpty()) {
            listener.reportNonFatalError(String.format("Unable to locate image %s for pole %s", pole.getUtilityId(), imageNum));
            return true;
        }
        
        for (ResourceMetadata rmeta : imgList) {
            InspectionEventResourceSearchParams ierparams = new InspectionEventResourceSearchParams();
            ierparams.setInspectionEventId(evt.getId());
            ierparams.setResourceId(rmeta.getResourceId());
            InspectionEventResource ier = CollectionsUtilities.firstItemIn(svcs.inspectionEventResources().search(svcs.token(), ierparams));
            if (ier == null) {
                ier = new InspectionEventResource();
                ier.setAssetId(pole.getId());
                ier.setId(UUID.randomUUID().toString());
                ier.setInspectionEventId(evt.getId());
                ier.setOrderNumber(evt.getOrderNumber());
                ier.setPolygons(polys);
                ier.setResourceId(rmeta.getResourceId());
                ier.setSiteId(evt.getSiteId());
            } else {
                data.addInspectionEventResource(ier, false);
            }
        }
        
        return true;
    }
    
    private static final String XPATH_OBJECT = "/annotation/object";
    private static final String XPATH_XMAX = "/bndbox/xmax";
    private static final String XPATH_XMIN = "/bndbox/xmin";
    private static final String XPATH_YMAX = "/bndbox/ymax";
    private static final String XPATH_YMIN = "/bndbox/ymin";
    
    static List<InspectionEventPolygon> parseForPolygons(InspectionEvent evt, InputStream is)
        throws IOException, SAXException
    {
        List<InspectionEventPolygon> list = new LinkedList();
        PoleXMLDocumentHandler handler = new PoleXMLDocumentHandler();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(is));
        InspectionEventPolygon poly;
        int i = 0;
        for (Observation o : handler.getObservations()) {
            i++;
            poly = new InspectionEventPolygon();
            poly.setGeometry(o.points());
            poly.setId(UUID.randomUUID().toString());
            poly.setName(String.format("%s: %d", o.getName(), i));
            poly.setSeverity(evt.getSeverity());
            poly.setText(o.getName());
            list.add(poly);
        }
        return list;
    }
}
