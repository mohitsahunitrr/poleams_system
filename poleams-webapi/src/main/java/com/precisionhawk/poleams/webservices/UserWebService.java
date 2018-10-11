package com.precisionhawk.poleams.webservices;

import com.precisionhawk.poleams.bean.User;
import io.swagger.oas.annotations.Operation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author pchapman
 */
@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserWebService {
    
    //FIXME: Replace with something more rhobust
    @GET
    @Path("/authenticate")
    @Operation(summary = "Authenticate a user", description = "Authenticates a user.")
    public User authenticate(@QueryParam("login") String login, @QueryParam("passhash") String passhash);
}
