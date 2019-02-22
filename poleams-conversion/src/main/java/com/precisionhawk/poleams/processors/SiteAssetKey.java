package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.domain.Asset;
import com.precisionhawk.ams.domain.Site;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import java.util.Objects;

/**
 *
 * @author pchapman
 */
public class SiteAssetKey {
    private String siteId;
    private String utilityId;

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getUtilityId() {
        return utilityId;
    }

    public void setUtilityId(String utilityId) {
        this.utilityId = utilityId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.siteId);
        hash = 29 * hash + Objects.hashCode(this.utilityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SiteAssetKey other = (SiteAssetKey) obj;
        if (!Objects.equals(this.siteId, other.siteId)) {
            return false;
        }
        if (!Objects.equals(this.utilityId, other.utilityId)) {
            return false;
        }
        return true;
    }

    public SiteAssetKey(String siteId, String utilityId) {
        this.siteId = siteId;
        this.utilityId = utilityId;
    }
    
    public SiteAssetKey(Site site, String utilityId) {
        this(site.getId(), utilityId);
    }
    
    public SiteAssetKey(Asset asset) {
        this.siteId = asset.getSiteId();
        if (asset instanceof Pole) {
            this.utilityId = ((Pole) asset).getUtilityId();
        } else if (asset instanceof TransmissionStructure) {
            this.utilityId = ((TransmissionStructure)asset).getStructureNumber();
        } else {
            throw new IllegalArgumentException("Invalid asset");
        }
    }
}
