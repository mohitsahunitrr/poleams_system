package com.precisionhawk.poleams.webservices;

import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSummary;
import com.precisionhawk.poleams.domain.SubStation;
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
@Path("/subStation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SubStationWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new substation record", description = "Creates a new substation.  If unique ID is not populated, it will be populated in the returned object.")
    SubStation create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            SubStation subStation
    );
    
    @GET
    @Path("/{subStationId}")
    @Operation(summary = "Get substation By ID", description = "Gets substation by unique ID")
    SubStation retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("subStationId") String id);
    
    @GET
    @Operation(summary = "Get all substations", description = "Gets all substations.")
    List<SubStation> retrieveAll(@Parameter(required = true) @HeaderParam("Authorization") String authToken);
    
    @GET
    @Path("/{subStationId}/summary")
    @Operation(summary = "Get substation summary", description = "Gets a summary of a substation.")
    SubStationSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("subStationId") String id);

    @POST
    @Path("/search")
    @Operation(summary = "Search substations", description = "Get a list of substations by search criteria.")
    List<SubStation> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            SubStationSearchParameters searchParams);
    
    @POST
    @Operation(summary = "Updates a substation.", description = "Updates an existing substation record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            SubStation subStation
    );    
}
