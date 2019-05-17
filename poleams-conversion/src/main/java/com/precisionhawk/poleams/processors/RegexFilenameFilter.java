package com.precisionhawk.poleams.processors;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author pchapman
 */
public class RegexFilenameFilter implements FilenameFilter {
    
    private final String regex;
    
    public RegexFilenameFilter(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name != null && name.matches(regex);
    }
    
}
