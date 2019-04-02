package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.ComponentSearchParams;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.ams.webservices.WebService;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author pchapman
 */
@Path("/component")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ComponentWebService extends WebService {
    @GET
    @Path("/{componentId}")
    @Operation(summary = "Get Component", description = "Gets a component by unique ID.")
    Component retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("componentId") String id);
    
    @POST
    @Path("/search")
    @Operation(summary = "Search components", description = "Searches components based on provided criteria.")
    List<Component> query(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ComponentSearchParams params);
    
    @PUT
    @Operation(summary = "Create component", description = "Creates a new component.")
    Component create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Component component);
        
    @DELETE
    @Path("{componentId}")
    @Operation(summary = "Delete Component", description = "Deletes the component.")
    void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("componentId") String id);
    
    @POST
    @Operation(summary = "Update component", description = "Updates an existing component.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Component component);    
}
