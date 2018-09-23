package com.precisionhawk.poleams.webservices;

import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author <a href="mail:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Path("/resource")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ResourceWebService extends WebService {
    
    @DELETE
    @Path("{resourceId}")
    @Operation(summary = "Delete Resource", description = "Deletes the resource.")
    void delete(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("resourceId") String id);
    
    @GET
    @Path("{resourceId}")
    @Operation(summary = "Retrieve Resource", description = "Retrieve the metadata about a resource by unique ID.")
    ResourceMetadata retrieve(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("resourceId") String id);
    
    @POST
    @Path("/search")
    @Operation(summary = "Search Resources", description = "Retrieves resource metadata for resources that match the indicated criteria.")
    List<ResourceMetadata> query(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceSearchParameters params);
    
    @POST
    @Path("/search/summary")
    @Operation(summary = "Search Resources", description = "Retrieves resource metadata for resources that match the indicated criteria.")
    List<ResourceSummary> querySummaries(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceSearchParameters params);

    @PUT
    @Operation(summary = "Save new Metadata", description = "Saves the metadata for a resource.")
    ResourceMetadata insertResourceMetadata(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceMetadata meta);

    @POST
    @Operation(summary = "Update existing Metadata", description = "Saves the metadata for a resource.")
    ResourceMetadata updateResourceMetadata(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            ResourceMetadata meta);
    
    @POST
    @Path("{resourceId}/scale")
    @Operation(summary = "Scale Image", description = "For image type resources, returns a new ResourceMetadata created by scaling the original.  The original remains unchanged.")
    ResourceMetadata scale(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("resourceId") String resourceId, 
            ImageScaleRequest scaleRequest);
    
    @POST
    @Path("verify")
    @Operation(summary = "Verify Resources", description = "Verifies that the resources indicated by unique ID have been uploaded and stored in WindAMS.  Returns a mapping of resource IDs to true or false depending on existance in WindAMS.")
    Map<String, Boolean> verifyUploadedResources(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            List<String> resourceIDs);
    
    @GET
    @Path("{resourceId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Downloads the resource.", description = "Downloads the resource.")
    public Response downloadResource(@PathParam("resourceId") String resourceId);
    
    @POST
    @Path("{resourceId}/upload")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    @Operation(summary = "Uploads the resource.", description = "Uploads the resource.")
    public void uploadResource(
            @Parameter(required = true) @HeaderParam("Authorization") String authToken,
            @PathParam("resourceId") String resourceId,
            @Context HttpServletRequest request
    );
}
