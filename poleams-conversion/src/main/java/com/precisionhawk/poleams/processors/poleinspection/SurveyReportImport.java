package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.GeoPoint;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.SubStation;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.papernapkin.liana.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip A. Chapman
 */
final class SurveyReportImport implements Constants, SurveyReportConstants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyReportImport.class);
    
    private static final FilenameFilter EXCEL_SPREADSHEET_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xlsx");
        }
    };
    
    // No state data
    private SurveyReportImport() {} 

    private static File findMasterSurveyTemplate(ImportProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(EXCEL_SPREADSHEET_FILTER);
        File excelFile = null;
        if (files.length > 1) {
            listener.reportFatalError(String.format("Multiple excel files exist in directory \"%s\"", feederDir));
            return null;
        } else if (files.length == 0) {
            listener.reportFatalError(String.format("Master Survey Template does not exist in directory \"%s\" or is not readable", feederDir));
            return null;
        } else {
            return files[0];
        }
    }
    
    static boolean processMasterSurveyTemplate(Environment env, ImportProcessListener listener, InspectionData data, File feederDir) {
        File masterDataFile = findMasterSurveyTemplate(listener, feederDir);
        if (masterDataFile == null) {
            return false;
        }
        Workbook workbook = null;
        try {
            listener.setStatus(ImportProcessStatus.ProcessingMasterSurveyTemplate);
            workbook = XSSFWorkbookFactory.createWorkbook(masterDataFile, true);
            
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
            }
            
            // We now have enough to lookup an existing sub station
            data.setSubStation(lookupSubStationByFeederId(env, feederId));
            if (data.getSubStation() == null) {
            String subStationName = getCellDataAsString(row, FEEDER_NAME.x);
                if (subStationName == null || subStationName.isEmpty()) {
                    // We cannot create a nameless substation.
                    listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder Name.");
                    return false;
                }
                // Create a new substation.
                data.setSubStation(new SubStation());
                data.getSubStation().setId(UUID.randomUUID().toString());
                data.getSubStation().setHardeningLevel(getCellDataAsString(row, FEEDER_HARDENING_LVL.x));
                data.getSubStation().setFeederNumber(feederId);
                data.getSubStation().setName(subStationName);
                data.getSubStation().setOrganizationId(ORG_ID);
                data.getSubStation().setWindZone(StringUtil.getNullableString(getCellDataAsInteger(row, FEEDER_WIND_ZONE.x)));
                data.getDomainObjectIsNew().put(data.getSubStation().getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getSubStation().getId(), false);
                //TODO: Update SubStation data?
            }
            
            // We may now process pole rows.
            PoleWebService poleSvc = env.obtainWebService(PoleWebService.class);
            PoleInspectionWebService poleInspSvc = env.obtainWebService(PoleInspectionWebService.class);
            
            boolean dataFound = true;
            for (int rowIndex = FIRST_POLE_ROW; dataFound; rowIndex++) {
                dataFound = processPoleRow(env, poleSvc, poleInspSvc, listener, sheet.getRow(rowIndex), data);
            }
            
            data.setMasterDataFile(masterDataFile);
            
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
        }
    }

    private static boolean processPoleRow(
            Environment env, PoleWebService psvc, PoleInspectionWebService pisvc, ProcessListener listener,
            Row row, InspectionData data
        )
        throws IOException
    {
        String fplId = getCellDataAsId(row, COL_FPL_ID);
        if (fplId == null || fplId.isEmpty()) {
            listener.reportMessage(String.format("Now fplId found on row %d, end of data found", row.getRowNum()));
            return false;
        } else if ("X".equals(fplId.toUpperCase())) {
            return false;
        } else {
            Pole pole = lookupPoleByFPLId(env, psvc, fplId);
            boolean isNew = false;
            if (pole == null) {
                String poleNum = getCellDataAsId(row, COL_POLE_NUM_1);
                if (poleNum == null || poleNum.isEmpty()) {
                    listener.reportNonFatalError(String.format("The tower on row %d with FPL ID %s has no Object ID.", row.getRowNum(), fplId));
                    return true;
                }
                
                pole = new Pole();
                pole.setFPLId(fplId);
                pole.setId(poleNum);
                pole.setOrganizationId(ORG_ID);
                pole.setSubStationId(data.getSubStation().getId());
                isNew = true;
            }
            pole.setType(getCellDataAsString(row, COL_POLE_TYPE));
            pole.setSwitchNumber(getCellDataAsString(row, COL_POLE_SWITCH_NUM));
            pole.setTlnCoordinate(getCellDataAsString(row, COL_POLE_TLN_COORD));
            Double lat = getCellDataAsDouble(row, COL_POLE_LAT);
            Double lon = getCellDataAsDouble(row, COL_POLE_LON);
            if (lat != null && lon != null && (!lat.equals(0.0)) && (!lon.equals(0.0))) {
                GeoPoint p = pole.getLocation();
                if (p == null) {
                    p = new GeoPoint();
                    pole.setLocation(p);
                }
                p.setLatitude(lat);
                p.setLongitude(lon);
            }
            data.addPole(pole, isNew);
            
            PoleInspection inspection = null;
            if (!isNew) {
                // Attempt to find an existing inspection
                PoleInspectionSearchParameters params = new PoleInspectionSearchParameters();
                params.setPoleId(pole.getId());
                inspection = CollectionsUtilities.firstItemIn(pisvc.search(env.obtainAccessToken(), params));
            }
            if (inspection == null) {
                inspection = new PoleInspection();
                inspection.setId(UUID.randomUUID().toString());
                inspection.setOrganizationId(ORG_ID);
                inspection.setPoleId(pole.getId());
                inspection.setSubStationId(data.getSubStation().getId());
                data.addPoleInspection(pole, inspection, true);
            } else {
                data.addPoleInspection(pole, inspection, false);
            }
            inspection.setAccess(getCellDataAsBoolean(row, COL_POLE_ACCESS));
            inspection.setLatLongDelta(getCellDataAsDouble(row, COL_LAT_LONG_DELTA));
            
            return true;
        }
    }
    
    private static final DecimalFormat LONG_INT = new DecimalFormat("########0");
    
    public static String getCellDataAsId(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        } else {
            Object value = null;
            CellType ctype = cell.getCellType();
            switch (ctype) {
                case BOOLEAN:
                    value = cell.getBooleanCellValue();
                    break;
                case FORMULA:
                case NUMERIC:
                    Double d = cell.getNumericCellValue();
                    return d == null ? null : LONG_INT.format(d);
                case STRING:
                    value = cell.getStringCellValue();
            }
            return value == null ? null : String.valueOf(value);
        }
    }
    
    private static SubStation lookupSubStationByFeederId(Environment env, String feederId) throws IOException {
        SubStationSearchParameters params = new SubStationSearchParameters();
        params.setFeederNumber(feederId);
        List<SubStation> subStations = env.obtainWebService(SubStationWebService.class).search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(subStations);
    }

    private static Pole lookupPoleByFPLId(Environment env, PoleWebService svc, String fplId) throws IOException {
        PoleSearchParameters params = new PoleSearchParameters();
        params.setFPLId(fplId);
        List<Pole> poles = svc.search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(poles);
    }
}
