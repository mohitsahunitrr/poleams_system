package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

/**
 * A summary of information for a resource.
 *
 * @author pchapman
 */
public class ResourceSummary {
    
    @Schema(description="The media type of the resource. See https://en.wikipedia.org/wiki/Media_type")
    private String contentType;
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /** A URL from which the resource may be  downloaded. */
    @Schema(description="A URL from which the resource may be  downloaded.")
    private String downloadURL;
    public String getDownloadURL() {
        return downloadURL;
    }
    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    @Schema(description="A geographic point indicating where the resouce was obtained.")
    private GeoPoint location;
    public GeoPoint getLocation() {
        return location;
    }
    public void setLocation(GeoPoint location) {
        this.location = location;
    }
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Schema(description="Unqiue ID of the pole that was inspected.")
    private String poleId;
    public String getPoleId() {
        return poleId;
    }
    public void setPoleId(String poleId) {
        this.poleId = poleId;
    }

    @Schema(description="The unique ID of the resource.")
    private String resourceId;
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String id) {
        this.resourceId = id;
    }
    
    /** A URL from which a scaled version may be downloaded. */
    @Schema(description="If the resource is an image, a URL which can be used to dlownload the zoomify file created from the image.")
    private String scaledImageURL;
    public String getScaledImageURL() {
        return scaledImageURL;
    }
    public void setScaledImageURL(String scaledImageURL) {
        this.scaledImageURL = scaledImageURL;
    }

    @Schema(description="The substation to which the inspected pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }

    @Schema(description="The date and time the resource was obtained.")
    private ZonedDateTime timestamp;
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /** A URL from which the zoomified version may be  downloaded. */
    @Schema(description="If the resource is an image, a URL which can be used to dlownload the zoomify file created from the image.")
    private String zoomifyURL;
    public String getZoomifyURL() {
        return zoomifyURL;
    }
    public void setZoomifyURL(String zoomifyURL) {
        this.zoomifyURL = zoomifyURL;
    }    
}
