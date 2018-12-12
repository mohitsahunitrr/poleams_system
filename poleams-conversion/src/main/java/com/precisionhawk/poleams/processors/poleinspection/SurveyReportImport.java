package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.Feeder;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.WorkOrderStatuses;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.processors.FilenameFilters;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.papernapkin.liana.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.precisionhawk.poleams.webservices.FeederWebService;
import java.time.LocalDate;

/**
 *
 * @author Philip A. Chapman
 */
final class SurveyReportImport implements Constants, SurveyReportConstants {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyReportImport.class);

    private static void lookupFeederInspection(Environment env, InspectionData data) throws IOException {
        SiteInspectionSearchParams params = new SiteInspectionSearchParams();
        params.setOrderNumber(data.getOrderNumber());
        params.setSiteId(data.getFeeder().getId());
        data.setFeederInspection(CollectionsUtilities.firstItemIn(env.obtainWebService(FeederInspectionWebService.class).search(env.obtainAccessToken(), params)));
        
    }

    // No state data
    private SurveyReportImport() {} 

    private static File findMasterSurveyTemplate(ProcessListener listener, File feederDir) {
        File[] files = feederDir.listFiles(FilenameFilters.EXCEL_SPREADSHEET_FILTER);
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
    
    static boolean processMasterSurveyTemplate(Environment env, ProcessListener listener, InspectionData data, File feederDir) {
        File masterDataFile = findMasterSurveyTemplate(listener, feederDir);
        if (masterDataFile == null) {
            return false;
        }
        Workbook workbook = null;
        try {
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
            data.setFeeder(lookupSubStationByFeederId(env, feederId));
            if (data.getFeeder() == null) {
                String subStationName = getCellDataAsString(row, FEEDER_NAME.x);
                if (subStationName == null || subStationName.isEmpty()) {
                    // We cannot create a nameless substation.
                    listener.reportFatalError("Master Survey Template spreadsheet is missing Feeder Name.");
                    return false;
                }
                // Create a new substation.
                Feeder f = new Feeder();
                f.setId(UUID.randomUUID().toString());
                f.setHardeningLevel(getCellDataAsString(row, FEEDER_HARDENING_LVL.x));
                f.setFeederNumber(feederId);
                f.setName(subStationName);
                f.setOrganizationId(data.getOrganizationId());
                f.setWindZone(StringUtil.getNullableString(getCellDataAsInteger(row, FEEDER_WIND_ZONE.x)));
                data.setFeeder(f);
                data.getDomainObjectIsNew().put(f.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getFeeder().getId(), false);
            }
            
            data.setWorkOrder(env.obtainWebService(WorkOrderWebService.class).retrieveById(env.obtainAccessToken(), data.getOrderNumber()));
            if (data.getWorkOrder() == null) {
                WorkOrder wo = new WorkOrder();
                wo.setOrderNumber(data.getOrderNumber());
                wo.getSiteIds().add(data.getFeeder().getId());
                wo.setStatus(WorkOrderStatuses.Requested);
                wo.setType(WorkOrderTypes.DistributionLineInspection);
                data.setWorkOrder(wo);
                data.getDomainObjectIsNew().put(wo.getOrderNumber(), true);
            } else {
                boolean found = false;
                for (String id : data.getWorkOrder().getSiteIds()) {
                    if (data.getFeeder().getId().equals(id)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Add this site to those for the work order.
                    data.getWorkOrder().getSiteIds().add(data.getFeeder().getId());
                }
                data.getDomainObjectIsNew().put(data.getWorkOrder().getOrderNumber(), false);
            }
            
            lookupFeederInspection(env, data);
            if (data.getFeederInspection() == null) {
                FeederInspection fi = new FeederInspection();
                fi.setDateOfInspection(LocalDate.now());
                fi.setId(UUID.randomUUID().toString());
                fi.setOrderNumber(data.getOrderNumber());
                fi.setSiteId(data.getFeeder().getId());
                data.setFeederInspection(fi);
                data.getDomainObjectIsNew().put(fi.getId(), true);
            } else {
                data.getDomainObjectIsNew().put(data.getFeederInspection().getId(), false);
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
            listener.reportMessage(String.format("No fplId found on row %d, end of data found", row.getRowNum() + 1));
            return false;
        } else if ("X".equalsIgnoreCase(fplId)) {
            return false;
        } else {
            listener.reportMessage(String.format("Processing Pole with FPL ID \"%s\" in spreadsheet row %d", fplId, (row.getRowNum() + 1)));
            String poleNum = getCellDataAsId(row, COL_POLE_NUM_1);
            if ("N/A".equalsIgnoreCase(poleNum)) {
                listener.reportNonFatalError(String.format("The tower on row %d with FPL ID %s is marked \"N/A\" and is being skipped.", row.getRowNum() + 1, fplId));
                return true;
            }
            PoleSearchParams pparams = new PoleSearchParams();
            pparams.setUtilityId(fplId);
            Pole pole = CollectionsUtilities.firstItemIn(psvc.search(env.obtainAccessToken(), pparams));
            boolean isNew = false;
            if (pole == null) {   
                pole = new Pole();
                pole.setUtilityId(fplId);
                pole.setId(UUID.randomUUID().toString());
                pole.setSiteId(data.getFeeder().getId());
                isNew = true;
            }
            String s = getCellDataAsString(row, COL_POLE_TYPE);
            pole.setType(s == null ? null : new AssetType(s));
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
                AssetInspectionSearchParams piparams = new AssetInspectionSearchParams();
                piparams.setAssetId(pole.getId());
                inspection = CollectionsUtilities.firstItemIn(pisvc.search(env.obtainAccessToken(), piparams));
            }
            if (inspection == null) {
                inspection = new PoleInspection();
                inspection.setId(UUID.randomUUID().toString());
                inspection.setAssetId(pole.getId());
                inspection.setOrderNumber(data.getOrderNumber());
                inspection.setSiteId(data.getFeeder().getId());
                inspection.setSiteInspectionId(data.getFeederInspection().getId());
                data.addPoleInspection(pole, inspection, true);
            } else {
                data.addPoleInspection(pole, inspection, false);
            }
            inspection.setAccess(getCellDataAsBoolean(row, COL_POLE_ACCESS));
            inspection.setLatLongDelta(getCellDataAsDouble(row, COL_LAT_LONG_DELTA));
            
            return true;
        }
    }
    
    private static Feeder lookupSubStationByFeederId(Environment env, String feederId) throws IOException {
        FeederSearchParams params = new FeederSearchParams();
        params.setFeederNumber(feederId);
        List<Feeder> subStations = env.obtainWebService(FeederWebService.class).search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(subStations);
    }

    private static Pole lookupPoleByFPLId(Environment env, PoleWebService svc, String fplId) throws IOException {
        PoleSearchParams params = new PoleSearchParams();
        params.setUtilityId(fplId);
        List<Pole> poles = svc.search(env.obtainAccessToken(), params);
        return CollectionsUtilities.firstItemIn(poles);
    }
}
