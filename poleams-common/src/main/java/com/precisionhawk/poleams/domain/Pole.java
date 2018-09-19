package com.precisionhawk.poleams.domain;

import com.precisionhawk.poleams.bean.GeoPoint;
import io.swagger.oas.annotations.media.Schema;

/**
 * A power substation.
 * 
 * @author pchapman
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
    
    @Schema(description="Unique ID of the pole assigned by the Utility.")
    private String fplId;
    public String getFPLId() {
        return fplId;
    }
    public void setFPLId(String id) {
        this.fplId = id;
    }

    @Schema(description="The height of the pole.")
    private String height;
    public String getHeight() {
        return height;
    }
    public void setHeight(String poleHeight) {
        this.height = poleHeight;
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
    
    @Schema(description="The unique ID of the substation to which this pole is related.")
    private String subStationId;
    public String getSubStationId() {
        return subStationId;
    }
    public void setSubStationId(String subStationId) {
        this.subStationId = subStationId;
    }

    @Schema(description="The type of pole.")
    private String type;
    public String getType() {
        return type;
    }
    public void setType(String poleType) {
        this.type = poleType;
    }
    
    protected void populateFrom(Pole p) {
        setFPLId(p.getFPLId());
        setHeight(p.getHeight());
        setId(p.getId());
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
        setSubStationId(p.getSubStationId());
        setType(p.getType());
    }
}