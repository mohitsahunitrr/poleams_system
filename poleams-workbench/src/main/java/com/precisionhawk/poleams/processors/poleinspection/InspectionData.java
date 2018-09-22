package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
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
    
    private Map<String, Boolean> domainDataIsNew = new HashMap<>();
    public Map<String, Boolean> getDomainObjectIsNew() {
        return domainDataIsNew;
    }

    private Map<String, PoleData> poleData = new HashMap<>();
    public Map<String, PoleData> getPoleDataByFPLId() {
        return poleData;
    }
    
    private Map<String, PoleInspection> poleInspectionsByFPLId = new HashMap<>();
    public Map<String, PoleInspection> getPoleInspectionsByFPLId() {
        return poleInspectionsByFPLId;
    }

    private Map<String, List<ResourceMetadata>> poleResources = new HashMap<>();
    public Map<String, List<ResourceMetadata>> getPoleResources() {
        return poleResources;
    }
    
    private Map<String, File> resourceDataFiles = new HashMap<>();
    public Map<String, File> getResourceDataFiles() {
        return resourceDataFiles;
    }

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
    
    public void addPole(PoleData pole, boolean isNew) {
        poleData.put(pole.getFPLId(), pole);
        domainDataIsNew.put(pole.getId(), isNew);
    }
    
    public void addPoleInspection(Pole pole, PoleInspection inspection, boolean isNew) {
        poleInspectionsByFPLId.put(pole.getFPLId(), inspection);
        domainDataIsNew.put(inspection.getId(), isNew);
    }
    
    public void addResourceMetadata(ResourceMetadata rmeta, File dataFile, boolean isNew) {
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
        domainDataIsNew.put(rmeta.getResourceId(), isNew);
        resourceDataFiles.put(rmeta.getResourceId(), dataFile);
    }
 
}
