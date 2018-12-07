package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.domain.FeederInspection;
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
@Path("/feederInspection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface FeederInspectionWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new substation record", description = "Creates a new substation.  If unique ID is not populated, it will be populated in the returned object.")
    FeederInspection create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            FeederInspection subStation
    );
    
    @GET
    @Path("/{inspectionId}")
    @Operation(summary = "Get feeder inspection By ID", description = "Gets feeder inspection by unique ID")
    FeederInspection retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);
    
    @POST
    @Path("/search")
    @Operation(summary = "Search feeder inspections", description = "Get a list of feeder inspections by search criteria.")
    List<FeederInspection> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            SiteInspectionSearchParams searchParams);
    
    @GET
    @Path("/{inspectionId}/summary")
    @Operation(summary = "Get feeder summary", description = "Gets a summary of a feeder inspection.")
    FeederInspectionSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);
    
    @POST
    @Operation(summary = "Updates a feeder inspection.", description = "Updates an existing feeder inspection record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            FeederInspection inspection
    );    
}
