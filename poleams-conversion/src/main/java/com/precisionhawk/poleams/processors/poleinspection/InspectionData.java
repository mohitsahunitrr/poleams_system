package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.SubStation;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Philip A. Chapman
 */
public class InspectionData {
    
    private final Map<String, Boolean> domainDataIsNew = new HashMap<>();
    public Map<String, Boolean> getDomainObjectIsNew() {
        return domainDataIsNew;
    }

    private final Map<String, Pole> poleData = new HashMap<>();
    public Map<String, Pole> getPoleDataByFPLId() {
        return poleData;
    }
    
    private final Map<String, PoleInspection> poleInspectionsByFPLId = new HashMap<>();
    public Map<String, PoleInspection> getPoleInspectionsByFPLId() {
        return poleInspectionsByFPLId;
    }

    private final Map<String, List<ResourceMetadata>> poleResources = new HashMap<>();
    public Map<String, List<ResourceMetadata>> getPoleResources() {
        return poleResources;
    }
    
    private final Map<String, File> resourceDataFiles = new HashMap<>();
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

    private final List<ResourceMetadata> subStationResources = new ArrayList<>();
    public List<ResourceMetadata> getSubStationResources() {
        return subStationResources;
    }
    
    public void addPole(Pole pole, boolean isNew) {
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
