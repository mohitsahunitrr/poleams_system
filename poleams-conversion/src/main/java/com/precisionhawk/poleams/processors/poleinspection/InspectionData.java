package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.processors.InspectionDataInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Philip A. Chapman
 */
public class InspectionData implements InspectionDataInterface {
    
    private final Map<String, Boolean> domainDataIsNew = new HashMap<>();
    @Override
    public Map<String, Boolean> getDomainObjectIsNew() {
        return domainDataIsNew;
    }

    private File masterDataFile;
    public File getMasterDataFile() {
        return masterDataFile;
    }
    public void setMasterDataFile(File masterDataFile) {
        this.masterDataFile = masterDataFile;
    }
    
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
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

    private Feeder feeder;
    public Feeder getFeeder() {
        return feeder;
    }
    public void setFeeder(Feeder feeder) {
        this.feeder = feeder;
    }
    
    private FeederInspection feederInspection;
    public FeederInspection getFeederInspection() {
        return feederInspection;
    }
    public void setFeederInspection(FeederInspection feederInspection) {
        this.feederInspection = feederInspection;
    }

    private final List<ResourceMetadata> feederResources = new ArrayList<>();
    public List<ResourceMetadata> getFeederResources() {
        return feederResources;
    }
    
    public void addPole(Pole pole, boolean isNew) {
        poleData.put(pole.getUtilityId(), pole);
        domainDataIsNew.put(pole.getId(), isNew);
    }
    
    public void addPoleInspection(Pole pole, PoleInspection inspection, boolean isNew) {
        poleInspectionsByFPLId.put(pole.getUtilityId(), inspection);
        domainDataIsNew.put(inspection.getId(), isNew);
    }
    
    public void addResourceMetadata(ResourceMetadata rmeta, File dataFile, boolean isNew) {
        if (rmeta.getAssetId() == null) {
            feederResources.add(rmeta);
        } else {
            List<ResourceMetadata> list = poleResources.get(rmeta.getAssetId());
            if (list == null) {
                list = new ArrayList<>();
                poleResources.put(rmeta.getAssetId(), list);
            }
            list.add(rmeta);
        }
        domainDataIsNew.put(rmeta.getResourceId(), isNew);
        resourceDataFiles.put(rmeta.getResourceId(), dataFile);
    }
 
    private String orderNumber;
    public String getOrderNumber() {
        return orderNumber;
    }
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    private WorkOrder workOrder;
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
}
