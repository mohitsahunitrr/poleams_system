package com.precisionhawk.poleams.processors.translineinspection;

import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.processors.InspectionDataInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class InspectionData implements InspectionDataInterface {
    
    private final Map<String, Boolean> domainDataIsNew = new HashMap<>();
    @Override
    public Map<String, Boolean> getDomainObjectIsNew() {
        return domainDataIsNew;
    }

    private final Map<String, TransmissionStructure> structureData = new HashMap<>();
    public Map<String, TransmissionStructure> getStructureDataByStructureNum() {
        return structureData;
    }
    
    private final Map<String, TransmissionStructureInspection> structureInspectionsByStructureNum = new HashMap<>();
    public Map<String, TransmissionStructureInspection> getStructureInspectionsByStructureNum() {
        return structureInspectionsByStructureNum;
    }

    private final Map<String, List<ResourceMetadata>> poleResources = new HashMap<>();
    public Map<String, List<ResourceMetadata>> getPoleResources() {
        return poleResources;
    }
    
    private final Map<String, File> resourceDataFiles = new HashMap<>();
    public Map<String, File> getResourceDataFiles() {
        return resourceDataFiles;
    }

    private TransmissionLine line;
    public TransmissionLine getLine() {
        return line;
    }
    public void setLine(TransmissionLine line) {
        this.line = line;
    }
    
    private TransmissionLineInspection lineInspection;
    public TransmissionLineInspection getLineInspection() {
        return lineInspection;
    }
    public void setLineInspection(TransmissionLineInspection lineInspection) {
        this.lineInspection = lineInspection;
    }

    private final List<ResourceMetadata> feederResources = new ArrayList<>();
    public List<ResourceMetadata> getFeederResources() {
        return feederResources;
    }
    
    public void addTransmissionStruture(TransmissionStructure struct, boolean isNew) {
        structureData.put(struct.getStructureNumber(), struct);
        domainDataIsNew.put(struct.getId(), isNew);
    }
    
    public void addTransmissionStructureInspection(TransmissionStructure struct, TransmissionStructureInspection inspection, boolean isNew) {
        structureInspectionsByStructureNum.put(struct.getStructureNumber(), inspection);
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
    
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    private WorkOrder workOrder;
    public WorkOrder getWorkOrder() {
        return workOrder;
    }
    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
}
