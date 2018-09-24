package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.SubStation;
import io.swagger.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A bean which summarizes SubStation data for display.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="A bean which summarizes SubStation data for display.")
public class SubStationSummary extends SubStation {
    
    @Schema(description="URL from which the feeder map for the substation can be downloaded.")
    private String feederMapDownloadURL;
    public String getFeederMapDownloadURL() {
        return feederMapDownloadURL;
    }
    public void setFeederMapDownloadURL(String feederMapDownloadURL) {
        this.feederMapDownloadURL = feederMapDownloadURL;
    }
    
    @Schema(description="URL from which the vegitation encroachment report for the feeder can be downloaded.")
    private String vegitationEncroachmentReportDownloadURL;
    public String getVegitationEncroachmentReportDownloadURL() {
        return vegitationEncroachmentReportDownloadURL;
    }
    public void setVegitationEncroachmentReportDownloadURL(String vegitationEncroachmentReportDownloadURL) {
        this.vegitationEncroachmentReportDownloadURL = vegitationEncroachmentReportDownloadURL;
    }

    @Schema(description="A map of pole summary objects mapped to pole FPL ID.")
    private Map<String, PoleSummary> polesByFPLId = new HashMap<>();
    public Map<String, PoleSummary> getPolesByFPLId() {
        return polesByFPLId;
    }
    public void setPolesByFPLId(Map<String, PoleSummary> polesByFPLId) {
        this.polesByFPLId = polesByFPLId;
    }

    @Schema(description="A map of pole summary objects mapped to pole FPL ID.")
    private Map<String, PoleInspectionSummary> poleInspectionsByFPLId = new HashMap<>();
    public Map<String, PoleInspectionSummary> getPoleInspectionsByFPLId() {
        return poleInspectionsByFPLId;
    }
    public void setPoleInspectionsByFPLId(Map<String, PoleInspectionSummary> polesByFPLId) {
        this.poleInspectionsByFPLId = poleInspectionsByFPLId;
    }
    
    public SubStationSummary() {}
    
    public SubStationSummary(SubStation sub, String feederMapDownloadURL, String vegitationEncroachmentReportDownloadURL, Collection<PoleSummary> poleSummaries, Collection<PoleInspectionSummary> poleInspectionSummaries) {
        this.feederMapDownloadURL = feederMapDownloadURL;
        this.vegitationEncroachmentReportDownloadURL = vegitationEncroachmentReportDownloadURL;
        setFeederNumber(sub.getFeederNumber());
        setHardeningLevel(sub.getHardeningLevel());
        setId(sub.getId());
        setName(sub.getName());
        setOrganizationId(sub.getOrganizationId());
        setWindZone(sub.getWindZone());
        Map<String, String> fplidByPoleId = new HashMap<>();
        for (PoleSummary ps : poleSummaries) {
            fplidByPoleId.put(ps.getId(), ps.getFPLId());
            polesByFPLId.put(ps.getFPLId(), ps);
        }
        for (PoleInspectionSummary pis : poleInspectionSummaries) {
            poleInspectionsByFPLId.put(fplidByPoleId.get(pis.getPoleId()), pis);
        }
    }
}
