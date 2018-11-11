/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.webservices.WebService;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
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
 *
 * @author Philip A. Chapman
 */
@Path("/workOrder")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface WorkOrderWebService  extends WebService
{
    @GET
    @Path("{orderNumber}")
    @Operation(summary = "Get Work Order", description = "Gets a work order by order number.")
    WorkOrder retrieveById(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("orderNumber") String orderNumber);
         
    @POST
    @Path("/search")
    @Operation(summary = "Search Work Orders", description = "Searches for work orders by search critiera.")
    List<WorkOrder> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            WorkOrderSearchParams searchBean);
     
    @PUT
    @Operation(summary = "Create Work Order", description = "Creates a new work order.")
    WorkOrder create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            WorkOrder workOrder);
    
    @DELETE
    @Path("{orderNumber}")
    @Operation(summary = "Delete Work Order", description = "Deletes a work order.")
    void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("orderNumber") String orderNumber);
     
    @POST
    @Operation(summary = "Update Work Order", description = "Updates an existing work order.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            WorkOrder workOrder);
}
