package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.domain.Feeder;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The interface of a web service providing APIs for accessing substation data.
 *
 * @author Philip A. Chapman
 */
@Path("/feeder")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface FeederWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new feeder record", description = "Creates a new feeder.  If unique ID is not populated, it will be populated in the returned object.")
    Feeder create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Feeder feeder
    );
    
    @GET
    @Path("/{feederId}")
    @Operation(summary = "Get feeder By ID", description = "Gets feeder by unique ID")
    Feeder retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("feederId") String id);
    
    @GET
    @Operation(summary = "Get all feeders", description = "Gets all feeders.")
    List<Feeder> retrieveAll(@Parameter(required = true) @HeaderParam("Authorization") String authToken);

    @POST
    @Path("/search")
    @Operation(summary = "Search feeders", description = "Get a list of feeders by search criteria.")
    List<Feeder> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            FeederSearchParams searchParams);
    
    @POST
    @Operation(summary = "Updates a feeder.", description = "Updates an existing feeder record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Feeder subStation
    );    
}
