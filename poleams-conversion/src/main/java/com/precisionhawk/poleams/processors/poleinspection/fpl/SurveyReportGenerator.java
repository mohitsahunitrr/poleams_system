package com.precisionhawk.poleams.processors.poleinspection.fpl;

import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.poledata.CommunicationsCable;
import com.precisionhawk.poleams.domain.poledata.PoleAnchor;
import static com.precisionhawk.poleams.processors.poleinspection.fpl.SurveyReportConstants.COL_FPL_ID;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import com.precisionhawk.poleams.webservices.FeederWebService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Populates the Master Survey Template for a substation and related pole and
 * pole inspection data.
 *
 * @author pchapman
 */
public class SurveyReportGenerator implements SurveyReportConstants {

    public static boolean process(Environment env, ProcessListener listener, String feederId, String orderNumber, File inFile, File outFile, boolean populateAll) {
        FeederSearchParams params = new FeederSearchParams();
        params.setFeederNumber(feederId);
        FeederWebService svc = env.obtainWebService(FeederWebService.class);
        try {
            List<Feeder> results = svc.search(env.obtainAccessToken(), params);
            Feeder ss = CollectionsUtilities.firstItemIn(results);
            if (ss == null) {
                listener.reportFatalError(String.format("No substation with feeder ID %s found", feederId));
                return false;
            }
            FeederInspectionWebService fisvc = env.obtainWebService(FeederInspectionWebService.class);
            SiteInspectionSearchParams fiparams = new SiteInspectionSearchParams();
            fiparams.setOrderNumber(orderNumber);
            fiparams.setSiteId(ss.getId());
            FeederInspection fi = CollectionsUtilities.firstItemIn(fisvc.search(env.obtainAccessToken(), fiparams));
            if (fi == null) {
                listener.reportFatalError(String.format("No feeder inspection found for feeder ID %s, work order number %s", feederId, orderNumber));
                return false;
            }
            FeederInspectionSummary summary = fisvc.retrieveSummary(env.obtainAccessToken(), fi.getId());
            return populateTemplate(env, listener, summary, inFile, outFile, populateAll);
        } catch (Throwable t) {
            listener.reportNonFatalException("", t);
            return false;
        }
    }
    
    public static boolean populateTemplate(Environment env, ProcessListener listener, FeederInspectionSummary summary, File inFile, File outFile, boolean populateAll) {
        if (inFile == null) {
            return false;
        }
        if (summary == null) {
            return false;
        }
        OutputStream outStream = null;
        Workbook workbook = null;
        try {
            workbook = XSSFWorkbookFactory.createWorkbook(inFile, false);
            
            // Find the "Survey Data" sheet.
            Sheet sheet = workbook.getSheet(SURVEY_SHEET);
            if (sheet == null) {
                listener.reportFatalError(String.format("Unable to locate Sheet\"%s\"", SURVEY_SHEET));
                return false;
            }
            
            // Get Feeder/Substation data
            Row row = sheet.getRow(FEEDER_NUM.y);
            if (row == null) {
                listener.reportFatalError("Master Survey Template spreadsheet does not contain data.");
                return false;
            }
            
            if (populateAll) {
                setCellData(row, FEEDER_NUM.x, summary.getFeederNumber());
                setCellData(row, FEEDER_NAME.x, summary.getName());
            } else {
                String feederId = getCellDataAsId(row, FEEDER_NUM.x);
                if (feederId == null || feederId.isEmpty()) {
                    listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder ID.");
                    return false;
                } else if (!feederId.equals(summary.getFeederNumber())) {
                    listener.reportFatalError(String.format("Master Survey Template spreadsheet is for the wrong Feeder, \"%s\".", feederId));
                }
            }
            
            String fplId;
            PoleInspectionSummary inspection;
            PoleSummary pole;
            
            //FIXME: This is a hack
            if (summary.getPoleInspectionsByFPLId().isEmpty()) {
                PoleInspection pi;
                AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                PoleInspectionWebService wsvc = env.obtainWebService(PoleInspectionWebService.class);
                for (PoleSummary p : summary.getPolesByFPLId().values()) {
                    params.setAssetId(p.getId());
                    pi = CollectionsUtilities.firstItemIn(wsvc.search(env.obtainAccessToken(), params));
                    if (pi != null) {
                        inspection = wsvc.retrieveSummary(env.obtainAccessToken(), pi.getId());
                        summary.getPoleInspectionsByFPLId().put(p.getUtilityId(), inspection);
                    }
                }
            }            

            if (populateAll) {
                List<PoleSummary> polesSortedBySeq = new ArrayList<>(summary.getPolesByFPLId().values());
                Collections.sort(polesSortedBySeq, new Comparator<PoleSummary>(){
                    @Override
                    public int compare(PoleSummary o1, PoleSummary o2) {
                        if (o1.getSequence() == null) {
                            if (o2.getSequence() == null) {
                                return 0;
                            } else {
                                return -1;
                            }
                        } else if (o2.getSequence() == null) {
                            return 1;
                        } else {
                            return Integer.compare(o1.getSequence(), o2.getSequence());
                        }                
                    }
                });
                int rowIndex = FIRST_POLE_ROW;
                for (PoleSummary ps : polesSortedBySeq) {
                    inspection = summary.getPoleInspectionsByFPLId().get(ps.getUtilityId());
                    row = ensureRow(sheet, rowIndex);
                    processRow(row, ps, inspection, true);
                    rowIndex++;
                }
            } else {
                boolean processing = true;
                int rowIndex = FIRST_POLE_ROW;
                while (processing) {
                    row = sheet.getRow(rowIndex);
                    fplId = getCellDataAsId(row, COL_FPL_ID);
                    if (fplId == null || "X".equals(fplId.toUpperCase())) {
                        processing = false;
                    } else {
                        listener.reportMessage(String.format("Processing FPL ID %s", fplId));
                        pole = summary.getPolesByFPLId().get(fplId);
                        inspection = summary.getPoleInspectionsByFPLId().get(fplId);
                        processRow(row, pole, inspection, false);
                        rowIndex++;
                    }
                }
            }
            
            listener.reportMessage(String.format("Saving the populated excel to %s", outFile.getAbsolutePath()));
            
            outStream = new FileOutputStream(outFile);
            workbook.write(outStream);
            
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
            IOUtils.closeQuietly(outStream);
        }
    }

    private static void processRow(Row row, PoleSummary pole, PoleInspectionSummary inspection, boolean populateAll) {
        if (pole != null) {
            if (populateAll) {
                setCellData(row, COL_FPL_ID, pole.getUtilityId());
                setCellData(row, COL_POLE_NUM_1, pole.getSequence());
                if (pole.getLocation() != null) {
                    setCellData(row, COL_POLE_LAT, pole.getLocation().getLatitude());
                    setCellData(row, COL_POLE_LON, pole.getLocation().getLongitude());
                }
            }
            setCellData(row, COL_POLE_NUM_2, pole.getSequence()); // Why copy pole num into another column?  IDK
            setCellData(row, COL_POLE_HEIGHT, pole.getLength());
            setCellData(row, COL_POLE_CLASS, pole.getPoleClass());
            setCellData(row, COL_POLE_SPAN_1_FRAMING, pole.getFraming());
            for (int i = 0; i < pole.getSpans().size() && i < COL_POLE_SPAN_LEN.length; i++) {
                setCellData(row, COL_POLE_SPAN_LEN[i], pole.getSpans().get(i).getLength());
            }
            setCellData(row, COL_POLE_SPAN_2_FRAMING, pole.getPullOffFraming());
            setCellData(row, COL_POLE_EQUIP_TYPE, pole.getEquipmentType());
            setCellData(row, COL_POLE_EQUIP_QUAN, pole.getEquipmentQuantity());
            setCellData(row, COL_POLE_STREETLIGHT, pole.getStreetLight());
            for (int i = 0; i < pole.getRisers().size() && i < COL_POLE_RISER_TYPE.length; i++) {
                setCellData(row, COL_POLE_RISER_TYPE[i], pole.getRisers().get(i));
            }
            setCellData(row, COL_POLE_CATV_ATTCHMNT_CNT, pole.getNumberOfCATVAttachments());
            setCellData(row, COL_POLE_CATV_TOTAL_SIZE, pole.getTotalSizeCATV());
            CommunicationsCable cable;
            for (int i = 0; i < pole.getCaTVAttachments().size() && i < COL_POLE_CATV_ATTCHMNT_DIAM.length; i++) {
                cable = pole.getCaTVAttachments().get(i);
                setCellData(row, COL_POLE_CATV_ATTCHMNT_DIAM[i], cable.getDiameter());
                setCellData(row, COL_POLE_CATV_ATTCHMNT_HEIGHT[i], cable.getHeight());
            }
            setCellData(row, COL_POLE_TELCO_ATTCHMNT_CNT, pole.getNumberOfTelComAttachments());
            setCellData(row, COL_POLE_TELCO_TOTAL_SIZE, pole.getTotalSizeTelCom());
            for (int i = 0; i < pole.getTelCommAttachments().size() && i < COL_POLE_TELCO_ATTCHMNT_DIAM.length; i++) {
                cable = pole.getTelCommAttachments().get(i);
                setCellData(row, COL_POLE_TELCO_ATTCHMNT_DIAM[i], cable.getDiameter());
                setCellData(row, COL_POLE_TELCO_ATTCHMNT_HEIGHT[i], cable.getHeight());
            }
            setCellData(row, COL_POLE_NUM_PHASES, pole.getNumberOfPhases());
            setCellData(row, COL_POLE_PRIMARY_WIRE_TYPE, pole.getPrimaryWireType());
            setCellData(row, COL_POLE_NEUTRAL_WIRE_TYPE, pole.getNeutralWireType());
            setCellData(row, COL_POLE_OPEN_WIRE_COUNT, pole.getNumberOfOpenWires());
            setCellData(row, COL_POLE_OPEN_WIRE_TYPE, pole.getOpenWireType());
            setCellData(row, COL_POLE_MULTIPLEX_TYPE, pole.getMultiplexType());
            PoleAnchor anchor;
            for (int i = 0; i < pole.getAnchors().size() && i < COL_GUY_ASSOC.length; i++) {
                anchor = pole.getAnchors().get(i);
                if (anchor.getGuyAssc() != null) {
                    setCellData(row, COL_GUY_ASSOC[i], anchor.getGuyAssc().toString());
                }
                setCellData(row, COL_GUY_BEARING[i], anchor.getBearing());
                setCellData(row, COL_GUY_DIAM[i], anchor.getStrandDiameter());
                setCellData(row, COL_GUY_LEAD_LEN[i], anchor.getLeadLength());
            }
        }
        if (inspection != null) {
            setCellData(row, COL_HORIZONTAL_POLE_LOADING, inspection.getHorizontalLoadingPercent());
            setCellData(row, COL_LAT_LONG_DELTA, inspection.getLatLongDelta());
        }
    }
}
