package com.precisionhawk.poleams.domain.poledata;

/**
 *
 * @author pchapman
 */
public class PoleAnchor {
    
    public enum GuyAssc {
        A1, B2, C3, D4;
        
        public static GuyAssc fromXMLString(String value) {
            switch (value) {
                case "Anchor 1" : return A1;
                case "Anchor 2" : return B2;
                case "Anchor 3" : return C3;
                case "Anchor 4" : return D4;
                default: return null;
            }
        }
    }
    
    private String leadLength;
    public String getLeadLength() {
        return leadLength;
    }
    public void setLeadLength(String leadLength) {
        this.leadLength = leadLength;
    }

    private String bearing;
    public String getBearing() {
        return bearing;
    }
    public void setBearing(String bearing) {
        this.bearing = bearing;
    }

    private GuyAssc guyAssc;
    public GuyAssc getGuyAssc() {
        return guyAssc;
    }
    public void setGuyAssc(GuyAssc guyAssc) {
        this.guyAssc = guyAssc;
    }

    private String strandDiameter;
    public String getStrandDiameter() {
        return strandDiameter;
    }
    public void setStrandDiameter(String strandDiameter) {
        this.strandDiameter = strandDiameter;
    }
}
