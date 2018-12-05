/*
 * All rights reserved.
 */

package com.precisionhawk.poleamsv0dot0.bean;

import java.io.Serializable;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class Dimension {
    
    private Double depth;
    public Double getDepth() {
        return depth;
    }
    public void setDepth(Double depth) {
        this.depth = depth;
    }

    private Double height;
    public Double getHeight() {
        return height;
    }
    public void setHeight(Double height) {
        this.height = height;
    }

    private Double width;
    public Double getWidth() {
        return width;
    }
    public void setWidth(Double width) {
        this.width = width;
    }
    
    public Dimension() {
        this(null, null, null);
    }
    
    public Dimension(Double width, Double height) {
        this(width, height, null);
    }
    
    public Dimension(Double width, Double height, Double depth) {
        this.depth = depth;
        this.height = height;
        this.width = width;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.depth != null ? this.depth.hashCode() : 0);
        hash = 53 * hash + (this.height != null ? this.height.hashCode() : 0);
        hash = 53 * hash + (this.width != null ? this.width.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Dimension other = (Dimension) obj;
        if (this.depth != other.depth && (this.depth == null || !this.depth.equals(other.depth))) {
            return false;
        }
        if (this.height != other.height && (this.height == null || !this.height.equals(other.height))) {
            return false;
        }
        if (this.width != other.width && (this.width == null || !this.width.equals(other.width))) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(Width: ");
        sb.append(fieldToString(width));
        sb.append(", Height: ");
        sb.append(fieldToString(height));
        sb.append(", Depth: ");
        sb.append(fieldToString(depth));
        sb.append(")");
        
        return sb.toString();
    }
    
    private static String fieldToString(Double fieldValue) {
        if (fieldValue == null) {
            return "NULL";
        } else {
            return fieldValue.toString();
        }
    }
}
