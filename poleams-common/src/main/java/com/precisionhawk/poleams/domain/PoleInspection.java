package com.precisionhawk.poleams.domain;

import com.precisionhawk.poleams.bean.PoleAnalysisLoadCase;
import io.swagger.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * A class representing the inspection of a pole.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="A class representing the inspection of a pole.")
public class PoleInspection implements Identifyable {
    
    @Schema(description="Unique internal ID of the inspection.")
    private String id;
    @Override
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    
    @Schema(description="If the technician had access to the tower.")
    private Boolean access;
    public Boolean getAccess() {
        return access;
    }
    public void setAccess(Boolean access) {
        this.access = access;
    }
    
    @Schema(description="Anchors pass analysis")
    private Boolean anchorsPass;
    public Boolean getAnchorsPass() {
        return anchorsPass;
    }
    public void setAnchorsPass(Boolean anchorsPass) {
        this.anchorsPass = anchorsPass;
    }
    
    @Schema(description="Brackets pass analysis")
    private Boolean bracketsPass;
    public Boolean getBracketsPass() {
        return bracketsPass;
    }
    public void setBracketsPass(Boolean bracketsPass) {
        this.bracketsPass = bracketsPass;
    }

    @Schema(description="The date the analysis was completed for the tower.")
    private LocalDate dateOfAnalysis;
    public LocalDate getDateOfAnalysis() {
        return dateOfAnalysis;
    }
    public void setDateOfAnalysis(LocalDate dateOfAnalysis) {
        this.dateOfAnalysis = dateOfAnalysis;
    }
    
    @Schema(description="Down guys pass analysis")
    private Boolean downGuysPass;
    public Boolean getDownGuysPass() {
        return downGuysPass;
    }
    public void setDownGuysPass(Boolean downGuysPass) {
        this.downGuysPass = downGuysPass;
    }

    @Schema(description="Horizontal loading percent result of the analysis.")
    private Integer horizontalLoadingPercent;
    public Integer getHorizontalLoadingPercent() {
        return horizontalLoadingPercent;
    }
    public void setHorizontalLoadingPercent(Integer horizontalLoadingPercent) {
        this.horizontalLoadingPercent = horizontalLoadingPercent;
    }
    
    @Schema(description="Insulators pass analysis")
    private Boolean insulatorsPass;
    public Boolean getInsulatorsPass() {
        return insulatorsPass;
    }
    public void setInsulatorsPass(Boolean insulatorsPass) {
        this.insulatorsPass = insulatorsPass;
    }
    
    private Double latLongDelta;
    public Double getLatLongDelta() {
        return latLongDelta;
    }
    public void setLatLongDelta(Double latLongDelta) {
        this.latLongDelta = latLongDelta;
    }

    @Schema(description="The load case used for analysing the pole.")
    private PoleAnalysisLoadCase loadCase;
    public PoleAnalysisLoadCase getLoadCase() {
        return loadCase;
    }
    public void setLoadCase(PoleAnalysisLoadCase loadCase) {
        this.loadCase = loadCase;
    }
    
    @Schema(description="The organization to which the substation and related poles belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    @Schema(description="Pass or fail judgement on analysis results.")
    private Boolean passedAnalysis;
    public Boolean getPassedAnalysis() {
        return passedAnalysis;
    }
    public void setPassedAnalysis(Boolean passedAnalysis) {
        this.passedAnalysis = passedAnalysis;
    }

    @Schema(description="Unqiue ID of the pole that was inspected.")
    private String poleId;
    public String getPoleId() {
        return poleId;
    }
    public void setPoleId(String poleId) {
        this.poleId = poleId;
    }

    @Schema(description="The substation to which the inspected pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }

    @Schema(description="Vertical loading percent result of the analysis.")
    private Integer verticalLoadingPercent;
    public Integer getVerticalLoadingPercent() {
        return verticalLoadingPercent;
    }
    public void setVerticalLoadingPercent(Integer verticalLoadingPercent) {
        this.verticalLoadingPercent = verticalLoadingPercent;
    }
    
    protected void populateFrom(PoleInspection pi) {
        setAccess(pi.getAccess());
        setAnchorsPass(pi.getAnchorsPass());
        setBracketsPass(pi.getBracketsPass());
        setDateOfAnalysis(pi.getDateOfAnalysis());
        setDownGuysPass(pi.getDownGuysPass());
        setHorizontalLoadingPercent(pi.getHorizontalLoadingPercent());
        setId(pi.getId());
        setInsulatorsPass(pi.getInsulatorsPass());
        setLatLongDelta(pi.getLatLongDelta());
        setLoadCase(pi.getLoadCase());
        setOrganizationId(pi.getOrganizationId());
        setPassedAnalysis(pi.passedAnalysis);
        setPoleId(pi.getPoleId());
        setSubStationId(pi.getSubStationId());
        setVerticalLoadingPercent(pi.getVerticalLoadingPercent());
    }
}
