package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.SubStationSummary;
import static com.precisionhawk.poleams.support.poi.ExcelUtilities.*;
import java.io.File;
import java.io.IOException;
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
public class MasterSurveyTemplatePopulator implements MasterSurveyTemplateConstants {
    
    public boolean populateTemplate(ProcessListener listener, SubStationSummary summary, File excelFile) {
        if (excelFile == null) {
            return false;
        }
        try {
            listener.setStatus(ProcessStatus.ProcessingMasterSurveyTemplate);
            Workbook workbook = XSSFWorkbookFactory.createWorkbook(excelFile, false);
            
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
            } else if (!feederId.equals(summary.getFeederNumber())) {
                listener.reportFatalError(String.format("Master Survey Template spreadsheet is for the wrong Feeder, \"%s\".", feederId));
            }
            
            //TODO:
            
            return true;
        } catch (InvalidFormatException | IOException ex) {
            listener.reportFatalException(ex);
            return false;
        }
    }
}
