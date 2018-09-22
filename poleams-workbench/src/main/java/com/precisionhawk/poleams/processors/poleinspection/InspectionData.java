package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class InspectionData {

    private SubStation subStation;
    public SubStation getSubStation() {
        return subStation;
    }
    public void setSubStation(SubStation subStation) {
        this.subStation = subStation;
    }

    private List<ResourceMetadata> subStationResources = new ArrayList<>();
    public List<ResourceMetadata> getSubStationResources() {
        return subStationResources;
    }
    public void setSubStationResources(List<ResourceMetadata> subStationResources) {
        this.subStationResources = subStationResources;
    }

    private Map<String, PoleData> poleData = new HashMap<>();
    public Map<String, PoleData> getPoleData() {
        return poleData;
    }
    public void setPoleData(Map<String, PoleData> poleData) {
        this.poleData = poleData;
    }
    
    private Map<String, Boolean> poleDataIsNew = new HashMap<>();
    public Map<String, Boolean> getPoleDataIsNew() {
        return poleDataIsNew;
    }
    public void setPoleDataIsNew(Map<String, Boolean> poleDataIsNew) {
        this.poleDataIsNew = poleDataIsNew;
    }

    private Map<String, List<ResourceMetadata>> poleResources = new HashMap<>();
    public Map<String, List<ResourceMetadata>> getPoleResources() {
        return poleResources;
    }
    public void setPoleResources(Map<String, List<ResourceMetadata>> poleResources) {
        this.poleResources = poleResources;
    }
    private Map<String, File> resourceDataFiles = new HashMap<>();
    public Map<String, File> getResourceDataFiles() {
        return resourceDataFiles;
    }
    public void setResourceDataFiles(Map<String, File> resourceDataFiles) {
        this.resourceDataFiles = resourceDataFiles;
    }
    
    public void addResourceMetadata(ResourceMetadata rmeta, boolean isNew) {
        if (rmeta.getPoleId() == null) {
            subStationResources.add(rmeta);
        } else {
            List<ResourceMetadata> list = poleResources.get(rmeta.getPoleId());
            if (list == null) {
                list = new ArrayList<>();
                poleResources.put(rmeta.getPoleId(), list);
            }
            list.add(rmeta);
        }
        poleDataIsNew.put(rmeta.getResourceId(), isNew);
    }
 
}
