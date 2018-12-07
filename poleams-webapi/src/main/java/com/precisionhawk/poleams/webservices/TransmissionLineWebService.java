package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.domain.TransmissionLine;
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
@Path("/transmissionLine")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TransmissionLineWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new transmission line record", description = "Creates a new transmission line.  If unique ID is not populated, it will be populated in the returned object.")
    TransmissionLine create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionLine line
    );
    
    @GET
    @Path("/{lineId}")
    @Operation(summary = "Get transmission line By ID", description = "Gets transmission line by unique ID")
    TransmissionLine retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("lineId") String id);
    
    @GET
    @Operation(summary = "Get all transmission lines", description = "Gets all transmission lines.")
    List<TransmissionLine> retrieveAll(@Parameter(required = true) @HeaderParam("Authorization") String authToken);

    @POST
    @Path("/search")
    @Operation(summary = "Search transmission lines", description = "Get a list of transmission lines by search criteria.")
    List<TransmissionLine> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionLineSearchParams searchParams);
    
    @POST
    @Operation(summary = "Updates a transmission line.", description = "Updates an existing transmission line record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            TransmissionLine line
    );    
}
