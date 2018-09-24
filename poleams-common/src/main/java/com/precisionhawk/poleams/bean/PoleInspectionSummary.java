package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.PoleInspection;
import io.swagger.oas.annotations.media.Schema;
import java.util.LinkedList;
import java.util.List;

/**
 * A summary of a pole inspection including related resources.
 *
 * @author Philip A. Chapman
 */
public class PoleInspectionSummary extends PoleInspection {
    
    @Schema(description="Anaysis result XML.")
    private ResourceSummary analysisResultXML;
    public ResourceSummary getAnalysisResultXML() {
        return analysisResultXML;
    }
    public void setAnalysisResultXML(ResourceSummary analysisResultXML) {
        this.analysisResultXML = analysisResultXML;
    }
    
    @Schema(description="The criticality of the pole in terms of needing upgrades. 1 - 5, 5 being most critical.")
    private Integer criticality;
    public Integer getCriticality() {
        return criticality;
    }
    public void setCriticality(Integer criticality) {
        this.criticality = criticality;
    }

    @Schema(description="A list of all images collected in drone flight, in no particular order.")
    private List<ResourceSummary> flightImages = new LinkedList<>();
    public List<ResourceSummary> getFlightImages() {
        return flightImages;
    }
    public void setFlightImages(List<ResourceSummary> flightImages) {
        this.flightImages = flightImages;
    }

    @Schema(description="A list of all images collected on the ground, in no particular order.")
    private List<ResourceSummary> groundImages = new LinkedList<>();
    public List<ResourceSummary> getGroundImages() {
        return groundImages;
    }
    public void setGroundImages(List<ResourceSummary> groundImages) {
        this.groundImages = groundImages;
    }

    @Schema(description="A list of other resources related to the inspection, in no particular order.")
    private List<ResourceSummary> otherResources = new LinkedList<>();
    public List<ResourceSummary> getOtherResources() {
        return otherResources;
    }
    public void setOtherResources(List<ResourceSummary> otherResources) {
        this.otherResources = otherResources;
    }

    @Schema(description="A list of all thermal images collected in drone flight, in no particular order.")
    private List<ResourceSummary> thermalImages = new LinkedList<>();
    public List<ResourceSummary> getThermalImages() {
        return thermalImages;
    }
    public void setThermalImages(List<ResourceSummary> thermalImages) {
        this.thermalImages = thermalImages;
    }
    
    public PoleInspectionSummary() {}
    
    public PoleInspectionSummary(PoleInspection pi) {
        populateFrom(pi);
    }
    
    public PoleInspectionSummary(PoleInspection pi, Integer criticality, ResourceSummary analysisResultXML, List<ResourceSummary> flightImages, List<ResourceSummary> groundImages, List<ResourceSummary> thermalImages, List<ResourceSummary> otherResources) {
        populateFrom(pi);
        this.analysisResultXML = analysisResultXML;
        this.criticality = criticality;
        this.flightImages = flightImages;
        this.groundImages = groundImages;
        this.otherResources = otherResources;
        this.thermalImages = thermalImages;
    }
}
