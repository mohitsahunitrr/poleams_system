package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceType;
import io.swagger.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

/**
 * A summary of information for a resource.
 *
 * @author Philip A. Chapman
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

    @Schema(description="Unqiue ID of the pole that was inspected.")
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
    
    @Schema(description="The type of resource being stored.")
    private ResourceType type;
    public ResourceType getType() {
        return type;
    }
    public void setType(ResourceType type) {
        this.type = type;
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
    
    public ResourceSummary() {}
    
    public ResourceSummary(ResourceMetadata rmeta, String downloadURL, String scaledImageURL, String zoomifyURL) {
        this.contentType = rmeta.getContentType();
        this.downloadURL = downloadURL;
        this.location = rmeta.getLocation();
        this.poleId = rmeta.getAssetId();
        this.poleInspectionId = rmeta.getAssetInspectionId();
        this.resourceId = rmeta.getResourceId();
        this.scaledImageURL = scaledImageURL;
        this.subStationId = rmeta.getSiteId();
        this.timestamp = rmeta.getTimestamp();
        this.type = rmeta.getType();
        this.zoomifyURL = zoomifyURL;
    }
}
