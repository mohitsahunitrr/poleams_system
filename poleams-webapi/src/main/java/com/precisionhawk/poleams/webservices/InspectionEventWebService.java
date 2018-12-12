/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.ams.webservices.WebService;
import com.precisionhawk.poleams.domain.InspectionEvent;
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
 * An interface implemented by web services that provide access and logic for
 * inspection events.
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Path("/inspectionEvent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface InspectionEventWebService extends WebService {

    @POST
    @Path("count")
    @Operation(summary="Retrieve a count of inspection events", description="Retrieve a count inspection events that match the given search criteria.")
    Long count(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            InspectionEventSearchParams searchParms);
    
    @GET
    @Path("{inspectionEventId}")
    @Operation(summary="Retrieve an inspection event", description="Retrieve an inspection event by ID.")
    InspectionEvent retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionEventId") String id);

    @POST
    @Path("search")
    @Operation(summary="Searches for inspection events", description="Searches for inspection events based on the given search criteria.")
    List<InspectionEvent> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            InspectionEventSearchParams searchParams);

    @PUT
    @Operation(summary="Create inspection event", description="Creates a new inspection event.")
    InspectionEvent create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            InspectionEvent event);
    
    @DELETE
    @Path("{inspectionEventId}")
    @Operation(summary="Delete inspection event", description="Deletes an existing inspection event.")
    void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("inspectionEventId") String id);
    
    @POST
    @Operation(summary="Update inspection event", description="Updates an existing inspection event.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            InspectionEvent event);
}
