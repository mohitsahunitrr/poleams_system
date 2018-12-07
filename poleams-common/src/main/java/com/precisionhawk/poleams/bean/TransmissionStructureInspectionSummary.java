package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import io.swagger.oas.annotations.media.Schema;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pchapman
 */
@Schema(description="A summary of a transmission structure inspection.")
public class TransmissionStructureInspectionSummary extends TransmissionStructureInspection {
    public TransmissionStructureInspectionSummary() {}
    
    public TransmissionStructureInspectionSummary(TransmissionStructureInspection insp) {
        setAssetId(insp.getAssetId());
        setDateOfInspection(insp.getDateOfInspection());
        setId(insp.getId());
        setOrderNumber(insp.getOrderNumber());
        setProcessedBy(insp.getProcessedBy());
        setReasonNotInspected(insp.getReasonNotInspected());
        setSiteId(insp.getSiteId());
        setSiteInspectionId(insp.getSiteInspectionId());
        setStatus(insp.getStatus());
        setType(insp.getType());
    }

    @Schema(description="A list of all images collected in drone flight, in no particular order.")
    private List<ResourceSummary> flightImages = new LinkedList<>();
    public List<ResourceSummary> getFlightImages() {
        return flightImages;
    }
    public void setFlightImages(List<ResourceSummary> flightImages) {
        this.flightImages = flightImages;
    }
}
