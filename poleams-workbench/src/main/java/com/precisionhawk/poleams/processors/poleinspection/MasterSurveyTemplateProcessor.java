package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.GeoPoint;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pchapman
 */
final class MasterSurveyTemplateProcessor implements Constants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MasterSurveyTemplateProcessor.class);
    
    private static final int COL_FPL_ID = 0;
    private static final int COL_POLE_NUM = 1;
    private static final int COL_POLE_TYPE = 2;
    private static final int COL_POLE_OWNER = 3;
    private static final int COL_POLE_ACCESS = 4;
    private static final int COL_POLE_HEIGHT = 6;
    private static final int COL_POLE_CLASS = 7;
    private static final int COL_POLE_FRAMING_1 = 8;
    private static final int COL_POLE_SPAN_LEN_1 = 9;
    private static final int COL_POLE_SPAN_LEN_2 = 10;
    private static final int COL_POLE_SPAN_LEN_3 = 11;
    private static final int COL_POLE_SPAN_LEN_4 = 12;
    private static final int COL_POLE_FRAMING_2 = 13;
    private static final int COL_POLE_EQUIP_TYPE = 14;
    private static final int COL_POLE_EQUIP_QUAN = 15;
    private static final int COL_POLE_STREETLIGHT = 16;
    private static final int COL_POLE_RISER_TYPE_1 = 17;
    private static final int COL_POLE_RISER_TYPE_2 = 18;
    private static final int COL_POLE_CATV_ATTCHMNT_CNT = 19;
    private static final int COL_POLE_CATV_TOTAL_SIZE = 20;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_1 = 21;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_1 = 22;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_2 = 23;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_2 = 24;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_3 = 25;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_3 = 26;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_4 = 27;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_4 = 28;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_5 = 29;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_5 = 30;
    private static final int COL_POLE_CATV_ATTCHMNT_HEIGHT_6 = 31;
    private static final int COL_POLE_CATV_ATTCHMNT_DIAM_6 = 32;
    private static final int COL_POLE_TELCO_ATTCHMNT_CNT = 33;
    private static final int COL_POLE_TELCO_TOTAL_SIZE = 34;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_1 = 35;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_1 = 36;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_2 = 37;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_2 = 38;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_3 = 39;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_3 = 40;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_4 = 41;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_4 = 42;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_5 = 43;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_5 = 44;
    private static final int COL_POLE_TELCO_ATTCHMNT_HEIGHT_6 = 45;
    private static final int COL_POLE_TELCO_ATTCHMNT_DIAM_6 = 46;
    private static final int COL_POLE_NUM_PHASES = 47;
    private static final int COL_POLE_PRIMARY_WIRE_TYPE = 48;
    private static final int COL_POLE_NEUTRAL_WIRE_TYPE = 49;
    private static final int COL_POLE_OPEN_WIRE_TYPE = 50;
    private static final int COL_POLE_OPEN_WIRE_COUNT = 51;
    private static final int COL_POLE_MULTIPLEX_TYPE = 52;
    private static final int COL_POLE_SWITCH_NUM = 53;
    private static final int COL_POLE_TLN_COORD = 54;
    private static final int COL_POLE_LAT = 55;
    private static final int COL_POLE_LON = 56;
    private static final int COL_POLE_CONTRACTOR_COMMENTS = 57;
    private static final Point FEEDER_HARDENING_LVL = new Point(13, 1);
    private static final Point FEEDER_NAME = new Point(5, 1);
    private static final Point FEEDER_NUM = new Point(1, 1);
    private static final Point FEEDER_WIND_ZONE = new Point(8, 1);
    private static final String SURVEY_SHEET = "Survey Data";
    
    private static final FilenameFilter EXCEL_SPREADSHEET_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xlsx");
        }
    };
    
    // No state data
    private MasterSurveyTemplateProcessor() {} 

    private static File findMasterSurveyTemplate(Environment environment, ProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(EXCEL_SPREADSHEET_FILTER);
        File excelFile = null;
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple excel files exist in directory \"%s\"", feederDir));
            return null;
        } else {
            if (excelFile == null) {
                listener.reportFatalError(String.format("Master Survey Template does not exist in directory \"%s\" or is not readable", feederDir));
                return null;
            } else {
                return files[1];
            }
        }
    }
    
    static boolean processMasterSurveyTemplate(Environment environment, ProcessListener listener, InspectionData inspectionData, File feederDir) {
        File masterDataFile = findMasterSurveyTemplate(environment, listener, feederDir);
        if (masterDataFile == null) {
            return false;
        }
        try {
            Workbook workbook = XSSFWorkbookFactory.createWorkbook(masterDataFile, true);
            listener.setStatus(ProcessStatus.ProcessingMasterSurveyTemplate);
            
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
            
            String feederId = getCellDataAsString(row, FEEDER_NUM.x);
            if (feederId == null || feederId.isEmpty()) {
                listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder ID.");
                return false;
            }
            
            // We now have enough to lookup an existing sub station
            inspectionData.setSubStation(lookupSubStationByFeederId(environment, feederId));
            
            String subStationName = getCellDataAsString(row, FEEDER_NAME.x);
            if (subStationName == null || subStationName.isEmpty()) {
                if (inspectionData.getSubStation() == null) {
                    // We cannot create a nameless substation.
                    listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder Name.");
                    return false;
                }
            } else if (inspectionData.getSubStation() == null) {
                // Create a new substation.
                inspectionData.setSubStation(new SubStation());
                inspectionData.getSubStation().setHardeningLevel(getCellDataAsString(row, FEEDER_HARDENING_LVL.x));
                inspectionData.getSubStation().setFeederNumber(feederId);
                inspectionData.getSubStation().setName(subStationName);
                inspectionData.getSubStation().setOrganizationId(ORG_ID);
                inspectionData.getSubStation().setWindZone(getCellDataAsString(row, FEEDER_WIND_ZONE.x));
//            } else if (!subStationName.equals(inspectionData.getSubStation().getName())) {
//
                //FIXME:
            }
            
            // We may now process pole rows.
            PoleWebService poleSvc = environment.obtainWebService(PoleWebService.class);
            
            boolean dataFound = true;
            for (int rowIndex = FEEDER_NUM.y + 1; dataFound; rowIndex++) {
                // Environment environment, ProcessListener listener, InspectionData inspectionData, 
                dataFound = processPoleRow(environment, poleSvc, listener, row, inspectionData);
            }
            
            return true;
        } catch (InvalidFormatException | IOException ex) {
            listener.reportFatalException(ex);
            return false;
        }
    }

    private static boolean processPoleRow(
            Environment environment, PoleWebService svc, ProcessListener listener,
            Row row, InspectionData inspectionData
        )
        throws IOException
    {
        String fplId = getCellDataAsString(row, COL_FPL_ID);
        if (fplId == null || fplId.isEmpty()) {
            listener.reportMessage(String.format("Now fplId found on row %d, end of data found", row.getRowNum()));
            return false;
        } else {
            PoleData pole = lookupPoleByFPLId(environment, svc, fplId);
            if (pole == null) {
                String poleNum = getCellDataAsString(row, COL_POLE_NUM);
                if (poleNum == null || poleNum.isEmpty()) {
                    listener.reportNonFatalError(String.format("The tower on row %d with FPL ID %s has no Object ID.", row.getRowNum(), fplId));
                    return true;
                }
                
                pole = new PoleData();
                pole.setFPLId(fplId);
                pole.setId(poleNum);
                inspectionData.getPoleDataIsNew().put(pole.getId(), Boolean.TRUE);
            } else {
                inspectionData.getPoleDataIsNew().put(pole.getId(), Boolean.FALSE);
            }
            pole.setType(getCellDataAsString(row, COL_POLE_TYPE));
            pole.setAccess(getCellDataAsBoolean(row, COL_POLE_ACCESS));
//TODO:                pole.setSwitchNumber(getCellDataAsString(row, COL_POLE_SWITCH_NUM));
//TODO:                pole.setTLNCoordinate(getCellDataAsString(row, COL_POLE_TLN_COORD));
            Double lat = getCellDataAsNumeric(row, COL_POLE_LAT);
            Double lon = getCellDataAsNumeric(row, COL_POLE_LON);
            if (lat != null && lon != null && (!lat.equals(0.0)) && (!lon.equals(0.0))) {
                GeoPoint p = pole.getLocation();
                if (p == null) {
                    p = new GeoPoint();
                    pole.setLocation(p);
                }
                p.setLatitude(lat);
                p.setLongitude(lon);
            }
            inspectionData.getPoleData().put(fplId, pole);
            return true;
        }
    }
    
    private static Boolean getCellDataAsBoolean(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        } else {
            return cell.getBooleanCellValue();
        }
    }
    
    private static Date getCellDataAsDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        } else {
            return cell.getDateCellValue();
        }
    }
    
    private static Double getCellDataAsNumeric(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        } else {
            return cell.getNumericCellValue();
        }
    }
    
    private static String getCellDataAsString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        } else {
            return cell.getStringCellValue();
        }
    }
    
    private static SubStation lookupSubStationByFeederId(Environment env, String feederId) throws IOException {
        SubStationSearchParameters params = new SubStationSearchParameters();
        params.setFeederNumber(feederId);
        List<SubStation> subStations = env.obtainWebService(SubStationWebService.class).search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(subStations);
    }

    private static PoleData lookupPoleByFPLId(Environment env, PoleWebService svc, String fplId) throws IOException {
        PoleSearchParameters params = new PoleSearchParameters();
        params.setFPLId(fplId);
        List<PoleData> poles = svc.search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(poles);
    }
}
