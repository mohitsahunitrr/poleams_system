package com.precisionhawk.poleams.support.poi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author pchapman
 */
public class ExcelUtilities {
    
    public static Boolean getCellDataAsBoolean(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
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
    
    public static void setCellData(Row row, int col, Boolean b) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            if (b == null) {
                return;
            }
            cell = row.createCell(col);
        }
        if (b == null) {
            cell.setCellValue((String)null);
        } else {
            cell.setCellValue(b);
        }
    }
    
    public static void setCellData(Row row, int col, LocalDate d) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            if (d == null) {
                return;
            }
            cell = row.createCell(col);
        }
        if (d == null) {
            cell.setCellValue((Date)null);
        } else {
            cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
    }
    
    public static void setCellData(Row row, int col, Float f) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            if (f == null) {
                return;
            }
            cell = row.createCell(col);
        }
        if (f == null) {
            cell.setCellValue((String)null);
        } else {
            cell.setCellValue(f);
        }
    }
    
    public static void setCellData(Row row, int col, Integer i) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            if (i == null) {
                return;
            }
            cell = row.createCell(col);
        }
        if (i == null) {
            cell.setCellValue((String)null);
        } else {
            cell.setCellValue(i);
        }
    }
    
    public static void setCellData(Row row, int col, String s) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            if (s == null) {
                return;
            }
            cell = row.createCell(col);
        }
        if (s == null) {
            cell.setCellValue((String)null);
        } else {
            cell.setCellValue(s);
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
    
    public static Row ensureRow(Sheet sheet, int index) {
        Row row = sheet.getRow(index);
        if (row == null) {
            row = sheet.createRow(index);
        }
        return row;
    }
}
