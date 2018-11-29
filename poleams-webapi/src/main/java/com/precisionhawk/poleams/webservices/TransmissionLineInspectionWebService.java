package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.TransmissionLineInspectionSummary;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
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
 *
 * @author pchapman
 */
@Path("/feederInspection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransmissionLineInspectionWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new transmission line inspection record", description = "Creates a new transmission line inspection.  If unique ID is not populated, it will be populated in the returned object.")
    TransmissionLineInspection create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionLineInspection inspection
    );
    
    @GET
    @Path("/{inspectionId}")
    @Operation(summary = "Get transmission line inspection By ID", description = "Gets transmission line inspection by unique ID")
    TransmissionLineInspection retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);
    
    @POST
    @Path("/search")
    @Operation(summary = "Search transmission line inspections", description = "Get a list of transmission line inspections by search criteria.")
    List<TransmissionLineInspection> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            SiteInspectionSearchParams searchParams);
    
    @GET
    @Path("/{inspectionId}/summary")
    @Operation(summary = "Get transmission line inspection summary", description = "Gets a summary of a transmission line inspection.")
    TransmissionLineInspectionSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);
    
    @POST
    @Operation(summary = "Updates a transmission line inspection.", description = "Updates an existing transmission line inspection record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionLineInspection inspection
    );    
}
