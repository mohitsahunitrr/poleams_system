package com.precisionhawk.poleams.domain;

import com.precisionhawk.poleams.bean.Dimension;
import com.precisionhawk.poleams.bean.GeoPoint;
import io.swagger.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 *
 * @author pchapman
 */
@Schema(description="Data related to a resource (image, sound, video, sensor readings, etc) gathered when an inspection is made.")
public final class ResourceMetadata {

    @Schema(description="Unique ID of the related pole.")
    private String poleId;
    public String getPoleId() {
        return poleId;
    }
    public void setPoleId(String poleId) {
        this.poleId = poleId;
    }

    @Schema(description="Unique ID of the related pole inspection.")
    private String poleInspectionId;
    public String getPoleInspectionId() {
        return poleInspectionId;
    }
    public void setPoleInspectionId(String poleInspectionId) {
        this.poleInspectionId = poleInspectionId;
    }
    
    @Schema(description="The media type of the resource. See https://en.wikipedia.org/wiki/Media_type")
    private String contentType;
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Schema(description="The unique ID of the resource.")
    private String resourceId;
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String id) {
        this.resourceId = id;
    }

    /**
     * Unique ID of the original resource image if this one is the result of
     * processing.
     */
    @Schema(description="If this resource was produced by modifying another resource (such as cropping an image), this is the unique ID of the source resource.")
    private String sourceResourceId;
    public String getSourceResourceId() {
        return sourceResourceId;
    }
    public void setSourceResourceId(String id) {
        this.sourceResourceId = id;
    }

    @Schema(description="The date and time the resource was obtained.")
    private ZonedDateTime timestamp;
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Schema(description="A geographic point indicating where the resouce was obtained.")
    private GeoPoint location;
    public GeoPoint getLocation() {
        return location;
    }
    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    @Schema(description="A name for the resource.")
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    @Schema(description="The organization.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    @Schema(description="The unique ID of the related substation.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }
    
    @Schema(description="The size of the resource, if applicable.")
    private Dimension size;
    public Dimension getSize() {
        return size;
    }
    public void setSize(Dimension size) {
        this.size = size;
    }

    @Schema(description="The status of the resource.")
    private ResourceStatus status;
    public ResourceStatus getStatus() {
	return status;
    }
    public void setStatus(ResourceStatus status) {
	this.status = status;
    }
    
    @Schema(description="The type of resource being stored.")
    private ResourceType type;
    public ResourceType getType() {
        return type;
    }
    public void setType(ResourceType type) {
        this.type = type;
    }
    
    /** The ID of the zoomified data stored in the repository. */
    @Schema(description="If the resource is an image, the unique ID of the zoomify file created from the image.")
    private String zoomifyId;
    public String getZoomifyId() {
        return zoomifyId;
    }
    public void setZoomifyId(String zoomifyId) {
        this.zoomifyId = zoomifyId;
    }
    
    public ResourceMetadata() {
        this.resourceId = UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceMetadata that = (ResourceMetadata) o;

        if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null) return false;

        return true;
    }
    
    @Override
    public int hashCode() {
        return resourceId != null ? resourceId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("Resource %s Name: %s Content Type: %s", resourceId, name, contentType);
    }
}
