package com.precisionhawk.poleamsv0dot0.domain;

import com.precisionhawk.poleamsv0dot0.bean.GeoPoint;
import com.precisionhawk.poleamsv0dot0.domain.poledata.PoleAnchor;
import com.precisionhawk.poleamsv0dot0.domain.poledata.PoleEquipment;
import com.precisionhawk.poleamsv0dot0.domain.poledata.PoleLight;
import com.precisionhawk.poleamsv0dot0.domain.poledata.PoleSpan;
import io.swagger.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A power substation.
 * 
 * @author Philip A. Chapman
 */
@Schema(description="Data related to power poles surveyed for a utility.")
public class Pole implements Identifyable {
    
    @Schema(description="Unique internal ID of the pole.")
    private String id;
    @Override
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
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
    
    @Schema(description="Unique ID of the pole assigned by the Utility.")
    private String fplId;
    public String getFPLId() {
        return fplId;
    }
    public void setFPLId(String id) {
        this.fplId = id;
    }

    @Schema(description="The length of the pole.")
    private Integer length;
    public Integer getLength() {
        return length;
    }
    public void setLength(Integer length) {
        this.length = length;
    }
    
    private List<PoleLight> lights = new LinkedList<>();
    public List<PoleLight> getLights() {
        return lights;
    }
    public void setLights(List<PoleLight> lights) {
        this.lights = lights;
    }
    
    @Schema(description="The location of the pole.")
    private GeoPoint location;
    public GeoPoint getLocation() {
        return location;
    }
    public void setLocation(GeoPoint location) {
        this.location = location;
    }
    
    @Schema(description="The organization to which the pole belong.")
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Schema(description="The class of the pole.")
    private String poleClass;
    public String getPoleClass() {
        return poleClass;
    }
    public void setPoleClass(String poleClass) {
        this.poleClass = poleClass;
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
    
    @Schema(description="The unique ID of the substation to which this pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }
    
    @Schema(description="Switch number.")
    private String switchNumber;
    public String getSwitchNumber() {
        return switchNumber;
    }
    public void setSwitchNumber(String switchNumber) {
        this.switchNumber = switchNumber;
    }
    
    @Schema(description="TLN Coordinate")
    private String tlnCoordinate;
    public String getTlnCoordinate() {
        return tlnCoordinate;
    }
    public void setTlnCoordinate(String tlnCoordinate) {
        this.tlnCoordinate = tlnCoordinate;
    }

    @Schema(description="The type of pole.")
    private String type;
    public String getType() {
        return type;
    }
    public void setType(String poleType) {
        this.type = poleType;
    }
    
    protected final void populateFrom(Pole p) {
        setAnchors(new ArrayList<>(p.getAnchors()));
        setDescription(p.getDescription());
        setEquipment(new ArrayList<>(p.getEquipment()));
        setFPLId(p.getFPLId());
        setLength(p.getLength());
        setId(p.getId());
        setLights(new ArrayList<>(p.getLights()));
        if (p.getLocation() == null) {
            p.setLocation(null);
        } else {
            setLocation(new GeoPoint());
            getLocation().setAccuracy(p.getLocation().getAccuracy());
            getLocation().setAltitude(p.getLocation().getAltitude());
            getLocation().setLatitude(p.getLocation().getLatitude());
            getLocation().setLongitude(p.getLocation().getLongitude());
        }
        setOrganizationId(p.getOrganizationId());
        setPoleClass(p.getPoleClass());        
        setRisers(new ArrayList<>(p.getRisers()));
        setSpans(new ArrayList<>(p.getSpans()));
        setSubStationId(p.getSubStationId());
        setSwitchNumber(p.getSwitchNumber());
        setTlnCoordinate(p.getTlnCoordinate());
        setType(p.getType());
    }
}