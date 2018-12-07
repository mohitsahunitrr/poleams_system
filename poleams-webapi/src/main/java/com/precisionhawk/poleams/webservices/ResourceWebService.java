package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.ResourceSummary;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author pchapman
 */
@Path("/resource")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ResourceWebService extends com.precisionhawk.ams.webservices.ResourceWebService
{
    @POST
    @Path("/search/summary")
    @Operation(summary = "Search Resources", description = "Retrieves resource metadata for resources that match the indicated criteria.")
    List<ResourceSummary> querySummaries(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceSearchParams params);
}
