package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.ExportState;
import com.precisionhawk.poleams.webservices.ExportWebService;
import javax.inject.Named;
import javax.ws.rs.core.Response;

/**
 *
 * @author pchapman
 */
@Named
public class ExportWebServiceImpl implements ExportWebService {

    @Override
    public ExportState requestPoleInspectionReport(String arg0, String arg1, String arg2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ExportState checkExportState(String arg0, String arg1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response downloadExport(String arg0, String arg1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
