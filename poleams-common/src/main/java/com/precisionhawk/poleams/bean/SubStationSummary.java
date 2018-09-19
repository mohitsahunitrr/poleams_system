package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.SubStation;
import io.swagger.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A bean which summarizes SubStation data for display.
 * 
 * @author pchapman
 */
@Schema(description="A bean which summarizes SubStation data for display.")
public class SubStationSummary extends SubStation {
    
    @Schema(description="URL from which the feeder map for the substation can be downloaded.")
    private String feederMapDownloadURL;
    public String getFeederMapDownloadURL() {
        return feederMapDownloadURL;
    }
    public void setFeedMapDownloadURL(String feederMapDownloadURL) {
        this.feederMapDownloadURL = feederMapDownloadURL;
    }

    @Schema(description="A map of pole summary objects mapped to pole FPL ID.")
    private Map<String, PoleSummary> polesByFPLId = new HashMap<>();
    public Map<String, PoleSummary> getPolesByFPLId() {
        return polesByFPLId;
    }
    public void setPolesByFPLId(Map<String, PoleSummary> polesByFPLId) {
        this.polesByFPLId = polesByFPLId;
    }
    
    public SubStationSummary() {}
    
    public SubStationSummary(SubStation sub, String feedMapDownloadURL, Collection<PoleSummary> poleSummaries) {
        this.feederMapDownloadURL = feedMapDownloadURL;
        setFeederNumber(sub.getFeederNumber());
        setHardeningLevel(sub.getHardeningLevel());
        setId(sub.getId());
        setName(sub.getName());
        setWindZone(sub.getWindZone());
        for (PoleSummary ps : poleSummaries) {
            polesByFPLId.put(ps.getFPLId(), ps);
        }
    }
}
