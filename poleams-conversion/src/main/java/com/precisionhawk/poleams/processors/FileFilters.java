package com.precisionhawk.poleams.processors;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 *
 * @author pchapman
 */
public interface FileFilters {
    
    public static final FilenameFilter CSV_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".csv");
        }
    };
    
    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };
    
    public static final FilenameFilter EXCEL_SPREADSHEET_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".xlsx");
        }
    };
    
    public static final FilenameFilter IMAGES_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith(".gif") || name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png");
        }
    };
}
