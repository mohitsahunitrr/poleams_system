/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices;

import com.precisionhawk.poleams.bean.ExportState;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Path("/export")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ExportWebService extends WebService {
    
    @GET
    @Path("/poleInspectionReport")
    @Operation(summary = "Request a pole inspection report.", description = "Requests that a pole inspection report Excel spreadsheet be generated for either all poles related to a substation or a single pole.  If poleId parameter is provided, substationId parameter is optional.")
    public ExportState requestPoleInspectionReport(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @QueryParam(value="subStationId") String subStationId,
            @QueryParam(value="poleId") String poleId
    );
    
    @GET
    @Path("/{uuid}/status")
    @Operation(summary = "Checks the status of an export request.", description = "Checks the status of an export request.")
    public ExportState checkExportState(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("uuid") String uuid
    );
    
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Downloads the result of an export request.", description = "Downloads the result of an export request.")
    public Response downloadExport(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("uuid") String uuid
    );
}
