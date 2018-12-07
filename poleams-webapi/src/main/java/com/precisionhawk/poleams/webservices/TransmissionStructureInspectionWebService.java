package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.TransmissionStructureInspectionSummary;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The interface of a web service providing APIs for accessing pole data.
 *
 * @author Philip A. Chapman
 */
@Path("/transmissionStructureInspection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransmissionStructureInspectionWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new transmission structure inspection record", description = "Creates a new transmission structure inspection.  If unique ID is not populated, it will be populated in the returned object.")
    TransmissionStructureInspection create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionStructureInspection inspection
    );
    
    @GET
    @Path("/{inspectionId}")
    @Operation(summary = "Get transmission structure inspection by ID", description = "Gets transmission structure inspection by unique ID")
    TransmissionStructureInspection retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);
    
    @GET
    @Path("/{inspectionId}/summary")
    @Operation(summary = "Get transmission structure inspection summary", description = "Gets a summary of a transmission structure inspection.")
    TransmissionStructureInspectionSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);

    @POST
    @Path("/search")
    @Operation(summary = "Search transmission structure inspections", description = "Get a list of transmission structure inspections by search criteria.")
    List<TransmissionStructureInspection> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            AssetInspectionSearchParams searchParams);
    
    @POST
    @Operation(summary = "Updates a transmission structure inspection.", description = "Updates an existing transmission structure inspection record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionStructureInspection inspection
    );

    @DELETE
    @Path("/{inspectionId}")
    @Operation(summary = "Delete transmission structure inspection by ID", description = "Deletes transmission structure inspection by unique ID")
    public void delete(String authToken, @PathParam("id") String id);
}
