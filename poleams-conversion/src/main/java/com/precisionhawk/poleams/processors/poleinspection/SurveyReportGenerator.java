package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSummary;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.domain.poledata.CommunicationsCable;
import com.precisionhawk.poleams.domain.poledata.PoleAnchor;
import static com.precisionhawk.poleams.processors.poleinspection.SurveyReportConstants.COL_FPL_ID;
import static com.precisionhawk.poleams.processors.poleinspection.SurveyReportImport.getCellDataAsId;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
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

/**
 * Populates the Master Survey Template for a substation and related pole and
 * pole inspection data.
 *
 * @author pchapman
 */
public class SurveyReportGenerator implements SurveyReportConstants {

    public static boolean process(Environment env, ProcessListener listener, String feederId, File inFile, File outFile) {
        SubStationSearchParameters params = new SubStationSearchParameters();
        params.setFeederNumber(feederId);
        SubStationWebService svc = env.obtainWebService(SubStationWebService.class);
        try {
            List<SubStation> results = svc.search(env.obtainAccessToken(), params);
            SubStation ss = CollectionsUtilities.firstItemIn(results);
            if (ss == null) {
                listener.reportFatalError(String.format("No substation with feeder ID %s found", feederId));
                return false;
            }
            SubStationSummary summary = svc.retrieveSummary(env.obtainAccessToken(), ss.getId());
            return populateTemplate(env, listener, summary, inFile, outFile);
        } catch (Throwable t) {
            listener.reportNonFatalException("", t);
            return false;
        }
    }
    
    public static boolean populateTemplate(Environment env, ProcessListener listener, SubStationSummary summary, File inFile, File outFile) {
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
            
            String feederId = getCellDataAsId(row, FEEDER_NUM.x);
            if (feederId == null || feederId.isEmpty()) {
                listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder ID.");
                return false;
            } else if (!feederId.equals(summary.getFeederNumber())) {
                listener.reportFatalError(String.format("Master Survey Template spreadsheet is for the wrong Feeder, \"%s\".", feederId));
            }
            
            String fplId;
            PoleInspectionSummary inspection;
            PoleSummary pole;
            
            //FIXME: This is a hack
            if (summary.getPoleInspectionsByFPLId().isEmpty()) {
                PoleInspection pi;
                PoleInspectionSearchParameters params = new PoleInspectionSearchParameters();
                PoleInspectionWebService wsvc = env.obtainWebService(PoleInspectionWebService.class);
                for (PoleSummary p : summary.getPolesByFPLId().values()) {
                    params.setPoleId(p.getId());
                    pi = CollectionsUtilities.firstItemIn(wsvc.search(env.obtainAccessToken(), params));
                    if (pi != null) {
                        inspection = wsvc.retrieveSummary(env.obtainAccessToken(), pi.getId());
                        summary.getPoleInspectionsByFPLId().put(p.getFPLId(), inspection);
                    }
                }
            }            

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
                    processRow(row, pole, inspection);
                    rowIndex++;
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

    private static void processRow(Row row, PoleSummary pole, PoleInspectionSummary inspection) {
        if (pole != null) {
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
                setCellData(row, COL_GUY_ASSOC[i], anchor.getGuyAssc().toString());
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
