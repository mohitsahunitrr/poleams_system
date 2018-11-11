package com.precisionhawk.poleams.webservices;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.ResourceSummary;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 *
 * @author pchapman
 */
public interface ResourceWebService extends com.precisionhawk.ams.webservices.ResourceWebService
{
    @POST
    @Path("/search/summary")
    @Operation(summary = "Search Resources", description = "Retrieves resource metadata for resources that match the indicated criteria.")
    List<ResourceSummary> querySummaries(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceSearchParams params);
}
