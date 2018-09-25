package com.precisionhawk.poleams.support.poi;

import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author pchapman
 */
public class ExcelUtilities {
    
    public static Boolean getCellDataAsBoolean(Row row, int col) {
        Cell cell = row.getCell(col);
        Object value = null;
        CellType ctype = cell.getCellType();
        switch (ctype) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
            case NUMERIC:
                value = cell.getNumericCellValue();
                break;
            case STRING:
                value = cell.getStringCellValue();
        }
        if (value == null) {
            return null;
        } else {
            try {
                // Special case
                if ("Y".equals(value) || "y".equals(value)) {
                    return Boolean.TRUE;
                }
                return Boolean.valueOf(value.toString());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(String.format("The value %s in row %d, column %d cannot be parsed as a numeric value.", value, row.getRowNum(), col));
            }
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
    
    public static Double getCellDataAsDouble(Row row, int col) {
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
                    return cell.getNumericCellValue();
                case STRING:
                    value = cell.getStringCellValue();
            }
            if (value == null) {
                return null;
            } else {
                try {
                    return Double.valueOf(value.toString());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(String.format("The value %s in row %d, column %d cannot be parsed as a decimal value.", value, row.getRowNum(), col));
                }
            }
        }
    }
    
    public static Integer getCellDataAsInteger(Row row, int col) {
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
                    return d == null ? null : d.intValue();
                case STRING:
                    value = cell.getStringCellValue();
            }
            if (value == null) {
                return null;
            } else {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(String.format("The value %s in row %d, column %d cannot be parsed as an integer value.", value, row.getRowNum(), col));
                }
            }
        }
    }
    
    public static String getCellDataAsString(Row row, int col) {
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
                    value = cell.getNumericCellValue();
                    break;
                case STRING:
                    value = cell.getStringCellValue();
            }
            return value == null ? null : String.valueOf(value);
        }
    }
    
}
