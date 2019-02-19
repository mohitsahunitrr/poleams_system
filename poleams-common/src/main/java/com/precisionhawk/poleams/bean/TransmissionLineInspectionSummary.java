package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import io.swagger.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pchapman
 */
@Schema(description="A summary of a transmission line inspection")
public class TransmissionLineInspectionSummary extends TransmissionLineInspection {

    //FIXME: nly feederInspection summaries contain a property for inspectionFlightVideos which is an array ...
    // it is missing under transmissionLineInspection summary.  Also transmissionLineInspection summary should
    // have a summaryReportDownloadURL but does not (both these properties are currently hard coded in dev).
    
    public TransmissionLineInspectionSummary() {
    }

    public TransmissionLineInspectionSummary(TransmissionLineInspection insp) {
        setDateOfInspection(insp.getDateOfInspection());
        setId(insp.getId());
        setOrderNumber(insp.getOrderNumber());
        setProcessedBy(insp.getProcessedBy());
        setSiteId(insp.getSiteId());
        setStatus(insp.getStatus());
        setType(insp.getType());
    }
    
    private Map<String, TransmissionStructureInspectionSummary> structureInspections = new HashMap<>();
    public Map<String, TransmissionStructureInspectionSummary> getStructureInspections() {
        return structureInspections;
    }
    public void setStructureInspections(Map<String, TransmissionStructureInspectionSummary> structureInspections) {
        this.structureInspections = structureInspections;
    }
    
    private Map<String, TransmissionStructure> structures = new HashMap<>();
    public Map<String, TransmissionStructure> getStructures() {
        return structures;
    }
    public void setStructures(Map<String, TransmissionStructure> structures) {
        this.structures = structures;
    }
}
