package com.precisionhawk.poleams.reporting;

import java.io.InputStream;

/**
 *
 * @author pchapman
 */
public interface ReportService {
    
    void setConfig(ReportingConfig config);
    
    InputStream generateReport(String reportId, String dataJSON, String outFileName) throws ReportingException;
    
}
