package com.precisionhawk.poleams.processors;

import com.precisionhawk.ams.domain.InspectionEventResource;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.InspectionEvent;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Philip A. Chapman
 */
public class InspectionData implements InspectionDataInterface {
    
    // Common Data
    
    private final Map<String, Boolean> domainDataIsNew = new HashMap<>();
    @Override
    public Map<String, Boolean> getDomainObjectIsNew() {
        return domainDataIsNew;
    }
    
    private String organizationId;
    public String getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    private final Map<String, List<ResourceMetadata>> assetResources = new HashMap<>();
    public Map<String, List<ResourceMetadata>> getAssetResources() {
        return assetResources;
    }
    
    private final Map<String, File> resourceDataFiles = new HashMap<>();
    public Map<String, File> getResourceDataFiles() {
        return resourceDataFiles;
    }

    private final List<ResourceMetadata> siteResources = new ArrayList<>();
    public List<ResourceMetadata> getSiteResources() {
        return siteResources;
    }
    
    public void addResourceMetadata(ResourceMetadata rmeta, File dataFile, boolean isNew) {
        if (rmeta.getAssetId() == null) {
            siteResources.add(rmeta);
        } else {
            List<ResourceMetadata> list = assetResources.get(rmeta.getAssetId());
            if (list == null) {
                list = new ArrayList<>();
                assetResources.put(rmeta.getAssetId(), list);
            }
            list.add(rmeta);
        }
        domainDataIsNew.put(rmeta.getResourceId(), isNew);
        resourceDataFiles.put(rmeta.getResourceId(), dataFile);
    }
 
    private String orderNumber;
    public String getCurrentOrderNumber() {
        return orderNumber;
    }
    public void setCurrentOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    private WorkOrder workOrder;
    public WorkOrder getCurrentWorkOrder() {
        return workOrder;
    }
    public void setCurrentWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    private Map<String, WorkOrder> workOrders = new HashMap();
    public Map<String, WorkOrder> getWorkOrders() {
        return workOrders;
    }
    public void setWorkOrders(Map<String, WorkOrder> workOrders) {
        this.workOrders = workOrders;
    }
    public void addWorkOrder(WorkOrder wo, boolean isNew) {
        workOrders.put(wo.getOrderNumber(), wo);
        domainDataIsNew.put(wo.getOrderNumber(), isNew);
    }

    // Distribution Line Data
    
    private File masterDataFile;
    public File getMasterDataFile() {
        return masterDataFile;
    }
    public void setMasterDataFile(File masterDataFile) {
        this.masterDataFile = masterDataFile;
    }

    private final Map<String, Pole> poleData = new HashMap<>();
    public Map<String, Pole> getPoleDataByFPLId() {
        return poleData;
    }
    
    private final Map<String, PoleInspection> poleInspectionsByFPLId = new HashMap<>();
    public Map<String, PoleInspection> getPoleInspectionsByFPLId() {
        return poleInspectionsByFPLId;
    }

    private Feeder feeder;
    public Feeder getCurrentFeeder() {
        return feeder;
    }
    public void setCurrentFeeder(Feeder feeder) {
        this.feeder = feeder;
    }
    private Map<String, Feeder> feedersByFeederNum = new HashMap();
    public Map<String, Feeder> getFeedersByFeederNum() {
        return feedersByFeederNum;
    }
    public void setFeedersByFeederNum(Map<String, Feeder> feeders) {
        this.feedersByFeederNum = feeders;
    }
    public void addFeeder(Feeder feeder, boolean isNew) {
        feedersByFeederNum.put(feeder.getFeederNumber(), feeder);
        domainDataIsNew.put(feeder.getId(), isNew);
    }
    
    private FeederInspection feederInspection;
    public FeederInspection getCurrentFeederInspection() {
        return feederInspection;
    }
    public void setCurrentFeederInspection(FeederInspection feederInspection) {
        this.feederInspection = feederInspection;
    }
    private Map<String, FeederInspection> feederInspections = new HashMap();
    public Map<String, FeederInspection> getFeederInspections() {
        return feederInspections;
    }
    public void setFeederInspections(Map<String, FeederInspection> feederInspectionsByFeederNum) {
        this.feederInspections = feederInspectionsByFeederNum;
    }
    public void addFeederInspection(FeederInspection feederInspection, boolean isNew) {
        feederInspections.put(feederInspection.getId(), feederInspection);
        domainDataIsNew.put(feederInspection.getId(), isNew);
    }
    
    private Map<String, InspectionEvent> inspectionEvents = new HashMap<>();
    public Map<String, InspectionEvent> getInspectionEvents() {
        return inspectionEvents;
    }
    public void setInspectionEvents(Map<String, InspectionEvent> inspectionEvents) {
        this.inspectionEvents = inspectionEvents;
    }
    public void addInspectionEvent(InspectionEvent evt, boolean isNew) {
        inspectionEvents.put(evt.getId(), evt);
        domainDataIsNew.put(evt.getId(), isNew);
    }
    
    private Map<String, InspectionEventResource> inspectionEventResources = new HashMap<>();
    public Map<String, InspectionEventResource> getInspectionEventResources() {
        return inspectionEventResources;
    }
    public void setInspectionEventResources(Map<String, InspectionEventResource> inspectionEventResources) {
        this.inspectionEventResources = inspectionEventResources;
    }
    public void addInspectionEventResource(InspectionEventResource res, boolean isNew) {
        inspectionEventResources.put(res.getId(), res);
        domainDataIsNew.put(res.getId(), isNew);
    }
    
    public void addPole(Pole pole, boolean isNew) {
        poleData.put(pole.getUtilityId(), pole);
        domainDataIsNew.put(pole.getId(), isNew);
    }
    
    public void addPoleInspection(Pole pole, PoleInspection inspection, boolean isNew) {
        poleInspectionsByFPLId.put(pole.getUtilityId(), inspection);
        domainDataIsNew.put(inspection.getId(), isNew);
    }

    // Transmission Line Data

    private final Map<String, TransmissionStructure> structureData = new HashMap<>();
    public Map<String, TransmissionStructure> getStructureDataByStructureNum() {
        return structureData;
    }
    
    private final Map<String, TransmissionStructureInspection> structureInspectionsByStructureNum = new HashMap<>();
    public Map<String, TransmissionStructureInspection> getStructureInspectionsByStructureNum() {
        return structureInspectionsByStructureNum;
    }
    
    private TransmissionLine line;
    public TransmissionLine getCurrentLine() {
        return line;
    }
    public void setCurrentLine(TransmissionLine line) {
        this.line = line;
    }
    private Map<String, TransmissionLine> linesByLineNum;
    public Map<String, TransmissionLine> getLinesByLineNum() {
        return linesByLineNum;
    }
    public void setLinesByLineNum(Map<String, TransmissionLine> transmissionLinesByLineNum) {
        this.linesByLineNum = transmissionLinesByLineNum;
    }
    public void addLine(TransmissionLine line, boolean isNew) {
        linesByLineNum.put(line.getLineNumber(), line);
        domainDataIsNew.put(line.getId(), isNew);
    }
    
    private TransmissionLineInspection lineInspection;
    public TransmissionLineInspection getCurrentLineInspection() {
        return lineInspection;
    }
    public void setCurrentLineInspection(TransmissionLineInspection lineInspection) {
        this.lineInspection = lineInspection;
    }
    private Map<String, TransmissionLineInspection> lineInspections = new HashMap();
    public Map<String, TransmissionLineInspection> getLineInspections() {
        return lineInspections;
    }
    public void setLineInspections(Map<String, TransmissionLineInspection> lineInspectionsByLineNum) {
        this.lineInspections = lineInspectionsByLineNum;
    }
    public void addLineInspection(TransmissionLineInspection inspection, boolean isNew) {
        lineInspections.put(inspection.getId(), inspection);
        domainDataIsNew.put(inspection.getId(), isNew);
    }
    
    public void addTransmissionStruture(TransmissionStructure struct, boolean isNew) {
        structureData.put(struct.getStructureNumber(), struct);
        domainDataIsNew.put(struct.getId(), isNew);
    }
    
    public void addTransmissionStructureInspection(TransmissionStructure struct, TransmissionStructureInspection inspection, boolean isNew) {
        structureInspectionsByStructureNum.put(struct.getStructureNumber(), inspection);
        domainDataIsNew.put(inspection.getId(), isNew);
    }
    
    public Collection<ResourceMetadata> allResources() {
        List<ResourceMetadata> list = new LinkedList<>();
        list.addAll(this.siteResources);
        for (String assetId : this.assetResources.keySet()) {
            list.addAll(this.assetResources.get(assetId));
        }
        return list;
    }
}
