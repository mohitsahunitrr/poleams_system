package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 *
 * @author <a href="mail:pchapman@pcsw.us">Philip A. Chapman</a>
 */
//FIXME: Merge this back in with WindAMS
@Schema(description="A geographic point.")
public class GeoPoint implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description="The accuracy of the reading.")
    private Double accuracy;
    public Double getAccuracy() {
        return accuracy;
    }
    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    @Schema(description="The altitude of the reading.")
    private Double altitude;
    /** Distance above mean sea level.  Should be measured in meters. */
    public Double getAltitude() {
        return altitude;
    }
    /** Distance above mean sea level.  Should be measured in meters. */
    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    @Schema(description="The latitude of the reading.")
    private Double latitude;
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @Schema(description="The longitude of the reading.")
    private Double longitude;
    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    
    public GeoPoint() {}

    public GeoPoint(Double latitude, Double longitude) {
        this(latitude, longitude, null, null);
    }

    public GeoPoint(Double latitude, Double longitude, Double altitude) {
        this(latitude, longitude, altitude, null);
    }
    
    public GeoPoint(Double latitude, Double longitude, Double altitude, Double accuracy) {
        this.accuracy = accuracy;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public GeoPoint(String coordinates) {
        if (coordinates.contains(":")) {
            String[] parts = coordinates.split(":");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid coordinate value " + coordinates);
            } else {
                latitude = Double.parseDouble(parts[0]);
                longitude = Double.parseDouble(parts[1]);
                accuracy = Double.parseDouble(parts[2]);
                altitude = Double.parseDouble(parts[3]);
            }
        } else if (coordinates.contains(",")) {
            String[] parts = coordinates.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid coordinate value " + coordinates);
            } else {
                latitude = Double.parseDouble(parts[0]);
                longitude = Double.parseDouble(parts[1]);
            }
        }
    }
    
    @Override
    public String toString() {
        return toStringLongForm();
    }
    
    public String toStringLongForm() {
        StringBuilder sb = new StringBuilder();
        if (latitude != null) {
            sb.append(Double.valueOf(latitude));
        }
        sb.append(':');
        if (longitude != null) {
            sb.append(Double.valueOf(longitude));
        }
        sb.append(':');
        if (accuracy != null) {
            sb.append(Double.valueOf(accuracy));
        }
        sb.append(':');
        if (altitude != null) {
            sb.append(Double.valueOf(altitude));
        }
        return sb.toString();
    }
    
    public String toStringShortForm() {
        StringBuilder sb = new StringBuilder();
        if (latitude != null) {
            sb.append(Double.valueOf(latitude));
        }
        sb.append(", ");
        if (longitude != null) {
            sb.append(Double.valueOf(longitude));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeoPoint other = (GeoPoint) obj;
        if (this.accuracy != other.accuracy && (this.accuracy == null || !this.accuracy.equals(other.accuracy))) {
            return false;
        }
        if (this.altitude != other.altitude && (this.altitude == null || !this.altitude.equals(other.altitude))) {
            return false;
        }
        if (this.latitude != other.latitude && (this.latitude == null || !this.latitude.equals(other.latitude))) {
            return false;
        }
        if (this.longitude != other.longitude && (this.longitude == null || !this.longitude.equals(other.longitude))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.accuracy != null ? this.accuracy.hashCode() : 0);
        hash = 41 * hash + (this.altitude != null ? this.altitude.hashCode() : 0);
        hash = 41 * hash + (this.latitude != null ? this.latitude.hashCode() : 0);
        hash = 41 * hash + (this.longitude != null ? this.longitude.hashCode() : 0);
        return hash;
    }
    
}
