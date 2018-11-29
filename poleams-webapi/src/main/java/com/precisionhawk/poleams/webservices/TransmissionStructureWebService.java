package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSummary;
import com.precisionhawk.poleams.domain.TransmissionStructure;
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
@Path("/transmissionStructure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransmissionStructureWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new transmission structure record", description = "Creates a new transmission structure.  If unique ID is not populated, it will be populated in the returned object.")
    TransmissionStructure create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionStructure structure
    );
        
    @GET
    @Path("/{id}")
    @Operation(summary = "Get transmission structure By ID", description = "Gets transmission structure by unique ID")
    TransmissionStructure retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("id") String id);
    
    @GET
    @Path("/{id}/summary")
    @Operation(summary = "Get transmission structure summary", description = "Gets a summary of a transmission structure.")
    TransmissionStructureSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("id") String id);

    @POST
    @Path("/search")
    @Operation(summary = "Search transmission structures", description = "Get a list of transmission structures by search criteria.")
    List<TransmissionStructure> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionStructureSearchParams searchParams);
    
    @POST
    @Operation(summary = "Updates a transmission structure.", description = "Updates an existing transmission structure record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionStructure structure
    );        

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete transmission structure By ID", description = "Deletes transmission structure by unique ID")
    void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("id") String id);
}
