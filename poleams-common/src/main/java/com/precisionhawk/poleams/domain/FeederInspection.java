package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.SiteInspection;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
public class FeederInspection extends SiteInspection {
    
    @Schema(description="URL from which the vegitation encroachment report for the feeder can be downloaded.")
    private String vegitationEncroachmentGoogleEarthURL;
    public String getVegitationEncroachmentGoogleEarthURL() {
        return vegitationEncroachmentGoogleEarthURL;
    }
    public void setVegitationEncroachmentGoogleEarthURL(String vegitationEncroachmentGoogleEarthURL) {
        this.vegitationEncroachmentGoogleEarthURL = vegitationEncroachmentGoogleEarthURL;
    }
    
}
