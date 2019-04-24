package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.ams.domain.ComponentInspection;
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
@Path("/componentInspection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ComponentInspectionWebService extends WebService {
    
    @PUT
    @Operation(summary = "Create a new pole inspection record", description = "Creates a new pole inspection.  If unique ID is not populated, it will be populated in the returned object.")
    ComponentInspection create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ComponentInspection inspection
    );
    
    @GET
    @Path("/{inspectionId}")
    @Operation(summary = "Get pole inspection by ID", description = "Gets pole inspection by unique ID")
    ComponentInspection retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id);

    @POST
    @Path("/search")
    @Operation(summary = "Search pole inspections", description = "Get a list of pole inspections by search criteria.")
    List<ComponentInspection> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ComponentInspectionSearchParams searchParams);
    
    @POST
    @Operation(summary = "Updates a pole inspection.", description = "Updates an existing pole inspection record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ComponentInspection inspection
    );

    @DELETE
    @Path("/{inspectionId}")
    @Operation(summary = "Delete pole inspection by ID", description = "Deletes pole inspection by unique ID")
    public void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionId") String id
    );
}
