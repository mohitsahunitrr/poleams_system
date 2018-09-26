/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface StatusWebService extends WebService {

    @GET
    public abstract Map<String, Object> retrieveStatus();

    @GET
    @Path("/status")
    public Map<String, Object> retrieveStatus2();
    
    @GET
    @Path("/poleams-services")
    public Map<String, Object> retrieveStatus3();
}
