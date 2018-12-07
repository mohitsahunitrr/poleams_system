package com.precisionhawk.poleams.webservices.client;

import com.precisionhawk.ams.webservices.SiteWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import java.io.IOException;

/**
 *
 * @author pchapman
 */
public final class WSClientHelper {

    public WSClientHelper() {}
    
    public WSClientHelper(Environment env) {
        this.env = env;
    }

    private Environment env;
    public Environment getEnv() {
        return env;
    }
    public void setEnv(Environment env) {
        this.env = env;
    }
    
    public String token() throws IOException {
        return env.obtainAccessToken();
    }
    
    public FeederInspectionWebService feederInspections() {
        return env.obtainWebService(FeederInspectionWebService.class);
    }
    
    public FeederWebService feeders() {
        return env.obtainWebService(FeederWebService.class);
    }
    
    public PoleInspectionWebService poleInspections() {
        return env.obtainWebService(PoleInspectionWebService.class);
    }
    
    public PoleWebService poles() {
        return env.obtainWebService(PoleWebService.class);
    }
    
    public ResourceWebService resources() {
        return env.obtainWebService(ResourceWebService.class);
    }
    
    public SiteWebService sites() {
        return env.obtainWebService(SiteWebService.class);
    }
    
    public TransmissionLineInspectionWebService transmissionLineInspections() {
        return env.obtainWebService(TransmissionLineInspectionWebService.class);
    }
    
    public TransmissionLineWebService transmissionLines() {
        return env.obtainWebService(TransmissionLineWebService.class);
    }
    
    public TransmissionStructureInspectionWebService transmissionStructureInspections() {
        return env.obtainWebService(TransmissionStructureInspectionWebService.class);
    }
    
    public TransmissionStructureWebService transmissionStructures() {
        return env.obtainWebService(TransmissionStructureWebService.class);
    }
    
    public WorkOrderWebService workOrders() {
        return env.obtainWebService(WorkOrderWebService.class);
    }
}
