package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.webservices.StatusWebService;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author pchapman
 */
@Named
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class RootWebService {
    
    @Inject private StatusWebService statusService;
    
    @GET
    Map<String, Object> retrieveStatus() {
        return statusService.retrieveStatus();
    }
}
