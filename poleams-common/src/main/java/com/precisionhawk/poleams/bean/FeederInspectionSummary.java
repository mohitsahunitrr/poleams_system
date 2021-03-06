package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import io.swagger.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A bean which summarizes SubStation data for display.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="A bean which summarizes feeder inspection data for display.")
public class FeederInspectionSummary extends Feeder {
    
    //TODO: Belongs to a SubStationInspection object
    @Schema(description="URL from which the anomaly map for the substation can be downloaded.")
    private String anomalyMapDownloadURL;
    public String getAnomalyMapDownloadURL() {
        return anomalyMapDownloadURL;
    }
    public void setAnomalyMapDownloadURL(String anomalyMapDownloadURL) {
        this.anomalyMapDownloadURL = anomalyMapDownloadURL;
    }
     //TODO: Belongs to a SubStationInspection object
   
    @Schema(description="URL from which the anomaly report for the substation can be downloaded.")
    private String anomalyReportDownloadURL;
    public String getAnomalyReportDownloadURL() {
        return anomalyReportDownloadURL;
    }
    public void setAnomalyReportDownloadURL(String anomalyReportDownloadURL) {
        this.anomalyReportDownloadURL = anomalyReportDownloadURL;
    }
    
    //TODO: Rename to "Circuit Map"
    @Schema(description="URL from which the feeder map for the substation can be downloaded.")
    private String feederMapDownloadURL;
    public String getFeederMapDownloadURL() {
        return feederMapDownloadURL;
    }
    public void setFeederMapDownloadURL(String feederMapDownloadURL) {
        this.feederMapDownloadURL = feederMapDownloadURL;
    }
    
    @Schema(description="Video(s) of the inspection flight(s).")
    private List<ResourceSummary> inspectionFlightVideos;
    public List<ResourceSummary> getInspectionFlightVideos() {
        return inspectionFlightVideos;
    }
    public void setInspectionFlightVideos(List<ResourceSummary> inspectionFlightVideos) {
        this.inspectionFlightVideos = inspectionFlightVideos;
    }
    
    @Schema(description="Summary report (PDF) of the inspection of the poles for the feeder.")
    private String summaryReportURL;
    public String getSummaryReportDownloadURL() {
        return summaryReportURL;
    }
    public void setSummaryReportDownloadURL(String summaryReportURL) {
        this.summaryReportURL = summaryReportURL;
    }
    
    //TODO: Belongs to a SubStationInspection object
    @Schema(description="URL from which the anomaly report for the substation can be downloaded.")
    private String surveyReportDownloadURL;
    public String getSurveyReportDownloadURL() {
        return surveyReportDownloadURL;
    }
    public void setSurveyReportDownloadURL(String surveyReportDownloadURL) {
        this.surveyReportDownloadURL = surveyReportDownloadURL;
    }

    //TODO: Belongs to a SubStationInspection object
    @Schema(description="URL from which the vegitation encroachment report for the feeder can be downloaded.")
    private String vegitationEncroachmentReportDownloadURL;
    public String getVegitationEncroachmentReportDownloadURL() {
        return vegitationEncroachmentReportDownloadURL;
    }
    public void setVegitationEncroachmentReportDownloadURL(String vegitationEncroachmentReportDownloadURL) {
        this.vegitationEncroachmentReportDownloadURL = vegitationEncroachmentReportDownloadURL;
    }

    //TODO: Belongs to a SubStationInspection object
    @Schema(description="URL from which the vegitation encroachment shape for the feeder can be downloaded.")
    private String vegitationEncroachmentShapeDownloadURL;
    public String getVegitationEncroachmentShapeDownloadURL() {
        return vegitationEncroachmentReportDownloadURL;
    }
    public void setVegitationEncroachmentShapeDownloadURL(String vegitationEncroachmentReportDownloadURL) {
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
        this.poleInspectionsByFPLId = polesByFPLId;
    }
    
    @Schema(description="URL from which the vegitation encroachment report for the feeder can be downloaded.")
    private String vegitationEncroachmentGoogleEarthURL;
    public String getVegitationEncroachmentGoogleEarthURL() {
        return vegitationEncroachmentGoogleEarthURL;
    }
    public void setVegitationEncroachmentGoogleEarthURL(String vegitationEncroachmentGoogleEarthURL) {
        this.vegitationEncroachmentGoogleEarthURL = vegitationEncroachmentGoogleEarthURL;
    }
    
    public FeederInspectionSummary() {}
    
    public FeederInspectionSummary(Feeder sub, FeederInspection finsp, Collection<PoleSummary> poleSummaries, Collection<PoleInspectionSummary> poleInspectionSummaries) {
        setFeederNumber(sub.getFeederNumber());
        setHardeningLevel(sub.getHardeningLevel());
        setId(sub.getId());
        setName(sub.getName());
        setOrganizationId(sub.getOrganizationId());
        setVegitationEncroachmentGoogleEarthURL(finsp.getVegitationEncroachmentGoogleEarthURL());
        setWindZone(sub.getWindZone());
        Map<String, String> fplidByPoleId = new HashMap<>();
        for (PoleSummary ps : poleSummaries) {
            fplidByPoleId.put(ps.getId(), ps.getUtilityId());
            polesByFPLId.put(ps.getUtilityId(), ps);
        }
        for (PoleInspectionSummary pis : poleInspectionSummaries) {
            poleInspectionsByFPLId.put(fplidByPoleId.get(pis.getAssetId()), pis);
        }
    }
}
