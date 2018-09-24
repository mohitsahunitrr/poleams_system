package com.precisionhawk.poleams.domain.poledata;

import com.precisionhawk.poleams.domain.Pole;
import java.util.LinkedList;
import java.util.List;

/**
 * Extended data for an inspected pole.
 *
 * @author Philip A. Chapman
 */
public class PoleData extends Pole {
    
    private Boolean access;
    public Boolean getAccess() {
        return access;
    }
    public void setAccess(Boolean access) {
        this.access = access;
    }
    
    private List<PoleAnchor> anchors = new LinkedList<>();
    public List<PoleAnchor> getAnchors() {
        return anchors;
    }
    public void setAnchors(List<PoleAnchor> anchors) {
        this.anchors = anchors;
    }
    
    private String description;
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    private List<PoleEquipment> equipment = new LinkedList<>();
    public List<PoleEquipment> getEquipment() {
        return equipment;
    }
    public void setEquipment(List<PoleEquipment> equipment) {
        this.equipment = equipment;
    }
    
    private List<PoleLight> lights = new LinkedList<>();
    public List<PoleLight> getLights() {
        return lights;
    }
    public void setLights(List<PoleLight> lights) {
        this.lights = lights;
    }
    
    private List<String> risers = new LinkedList<>();
    public List<String> getRisers() {
        return risers;
    }
    public void setRisers(List<String> risers) {
        this.risers = risers;
    }

    private List<PoleSpan> spans = new LinkedList<>();
    public List<PoleSpan> getSpans() {
        return spans;
    }
    public void setSpans(List<PoleSpan> spans) {
        this.spans = spans;
    }
    
    public PoleData() {}
    
    public PoleData(Pole p) {
        populateFrom(p);
    }
    
    public void updateFrom(Pole p) {
        populateFrom(p);
    }
}
