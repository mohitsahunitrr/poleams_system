package com.precisionhawk.poleams.webservices;

import com.precisionhawk.poleams.bean.PoleAnalysisImportJobState;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.domain.Pole;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * The interface of a web service providing APIs for accessing pole data.
 *
 * @author pchapman
 */
@Path("/pole")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PoleWebService {
    
    @PUT
    @Operation(summary = "Create a new pole record", description = "Creates a new pole.  If unique ID is not populated, it will be populated in the returned object.")
    Pole create(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Pole pole
    );
    
    @POST
    @Path("/{poleId}/importAnalysisExcel")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Import analysis data from Excel report.", description = "Imports analysis data from Excel report.")
    PoleAnalysisImportJobState importAnalysisExcel(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @Context HttpServletRequest request
    );
    
    @POST
    @Path("/{poleId}/importAnalysisXML")
    @Operation(summary = "Import analysis data from XML.", description = "Imports analysis data from XML.")
    PoleAnalysisImportJobState importAnalysisXML(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @Context HttpServletRequest request
    );
    
    @GET
    @Path("/{poleId}")
    @Operation(summary = "Get pole By ID", description = "Gets pole by unique ID")
    Pole retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("poleId") String id);
    
    @GET
    @Path("/{poleId}/summary")
    @Operation(summary = "Get pole summary", description = "Gets a summary of a pole.")
    PoleSummary retrieveSummary(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("poleId") String id);

    @POST
    @Path("/search")
    @Operation(summary = "Search poles", description = "Get a list of poles by search criteria.")
    List<Pole> search(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            PoleSearchParameters searchParams);
    
    @POST
    @Operation(summary = "Updates a pole.", description = "Updates an existing pole record.")
    void update(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            Pole pole
    );        
}
