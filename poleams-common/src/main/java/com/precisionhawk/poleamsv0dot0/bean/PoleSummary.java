package com.precisionhawk.poleams.bean;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.poledata.CommunicationsCable;
import io.swagger.oas.annotations.media.Schema;
import java.util.List;

/**
 * A displayable summary of Pole information.
 *
 * @author Philip A. Chapman
 */
@Schema(description="A displayable summary of Pole information.")
public class PoleSummary extends Pole {

    @Schema(description="A list of up to 6 cable TV attachments.  Only first span is desired.")
    private List<CommunicationsCable> caTVAttachments;
    public List<CommunicationsCable> getCaTVAttachments() {
        return caTVAttachments;
    }
    public void setCaTVAttachments(List<CommunicationsCable> caTVAttachments) {
        this.caTVAttachments = caTVAttachments;
    }
    
    @Schema(description="Circuit 1 Span Length 1")
    private String circuit1SpanLength1;
    public String getCircuit1SpanLength1() {
        return circuit1SpanLength1;
    }
    public void setCircuit1SpanLength1(String circuit1SpanLength1) {
        this.circuit1SpanLength1 = circuit1SpanLength1;
    }
    
    @Schema(description="Circuit 1 Span Length 2")
    private String circuit1SpanLength2;
    public String getCircuit1SpanLength2() {
        return circuit1SpanLength2;
    }
    public void setCircuit1SpanLength2(String circuit1SpanLength2) {
        this.circuit1SpanLength2 = circuit1SpanLength2;
    }

    @Schema(description="A count of equipment installed.")
    private Integer equipmentQuantity;
    public Integer getEquipmentQuantity() {
        return equipmentQuantity;
    }
    public void setEquipmentQuantity(Integer equipmentQuantity) {
        this.equipmentQuantity = equipmentQuantity;
    }
    
    @Schema(description="The type of equipment on the pole.  (Documents the primary equipment since a variety may exist.)")
    private String equipmentType;
    public String getEquipmentType() {
        return equipmentType;
    }
    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    @Schema(description="The framing of spans on the pole.  (Documents the primary since other spans may have different types of framing.)")
    private String framing;
    public String getFraming() {
        return framing;
    }
    public void setFraming(String framing) {
        this.framing = framing;
    }
    
    @Schema(description="Multiplex Type")
    private String multiplexType;
    public String getMultiplexType() {
        return multiplexType;
    }
    public void setMultiplexType(String multiplexType) {
        this.multiplexType = multiplexType;
    }
    
    @Schema(description="Neutral wire type")
    private String neutralWireType;
    public String getNeutralWireType() {
        return neutralWireType;
    }
    public void setNeutralWireType(String nutralWireType) {
        this.neutralWireType = nutralWireType;
    }
    
    @Schema(description="Number of Cable TV Attachments.  Only first span is desired.")
    private Integer numberOfCATVAttachments;
    public Integer getNumberOfCATVAttachments() {
        return numberOfCATVAttachments;
    }
    public void setNumberOfCATVAttachments(Integer numberOfCATVAttachments) {
        this.numberOfCATVAttachments = numberOfCATVAttachments;
    }
    
    @Schema(description="Number of Cable TV Attachments.  Only first span is desired.")
    private Integer numberOfTelComAttachments;
    public Integer getNumberOfTelComAttachments() {
        return numberOfTelComAttachments;
    }
    public void setNumberOfTelComAttachments(Integer numberOfTelComAttachments) {
        this.numberOfTelComAttachments = numberOfTelComAttachments;
    }
    
    @Schema(description="Number of Open Wires")
    private Integer numberOfOpenWires;
    public Integer getNumberOfOpenWires() {
        return numberOfOpenWires;
    }
    public void setNumberOfOpenWires(Integer numberOfOpenWires) {
        this.numberOfOpenWires = numberOfOpenWires;
    }

    @Schema(description="The number of phases of the primary span on the pole.)")
    private Integer numberOfPhases;
    public Integer getNumberOfPhases() {
        return numberOfPhases;
    }
    public void setNumberOfPhases(Integer numberOfPhases) {
        this.numberOfPhases = numberOfPhases;
    }
    
    @Schema(description="Open wire type.")
    private String openWireType;
    public String getOpenWireType() {
        return openWireType;
    }
    public void setOpenWireType(String openWireType) {
        this.openWireType = openWireType;
    }
    
    @Schema(description="The owner of the pole.")
    private String owner;
    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    @Schema(description="The type of the primary wire.")
    private String primaryWireType;
    public String getPrimaryWireType() {
        return primaryWireType;
    }
    public void setPrimaryWireType(String primaryWireType) {
        this.primaryWireType = primaryWireType;
    }
    
    @Schema(description="The span length of pull off 1")
    private String pullOff1SpanLength1;
    public String getPullOff1SpanLength1() {
        return pullOff1SpanLength1;
    }
    public void setPullOff1SpanLength1(String pullOff1SpanLength1) {
        this.pullOff1SpanLength1 = pullOff1SpanLength1;
    }

    @Schema(description="The span length of pull off 2")
    private String pullOff2SpanLength2;
    public String getPullOff2SpanLength2() {
        return pullOff2SpanLength2;
    }
    public void setPullOff2SpanLength2(String pullOff2SpanLength2) {
        this.pullOff2SpanLength2 = pullOff2SpanLength2;
    }
    
    @Schema(description="The framing of pull off 1")
    private String pullOffFraming;
    public String getPullOffFraming() {
        return pullOffFraming;
    }
    public void setPullOffFraming(String pullOffFraming) {
        this.pullOffFraming = pullOffFraming;
    }
    
    @Schema(description="The street light.")
    private String streetLight;
    public String getStreetLight() {
        return streetLight;
    }
    public void setStreetLight(String streetLight) {
        this.streetLight = streetLight;
    }
    
    @Schema(description="A list of up to 6 telecommunications attachments.  Only first span is desired.")
    private List<CommunicationsCable> telCommAttachments;
    public List<CommunicationsCable> getTelCommAttachments() {
        return telCommAttachments;
    }
    public void setTelCommAttachments(List<CommunicationsCable> telCommAttachments) {
        this.telCommAttachments = telCommAttachments;
    }
    
    @Schema(description="The total size of Cable TV.  Only first span is desired.")
    private Integer totalSizeCATV;
    public Integer getTotalSizeCATV() {
        return totalSizeCATV;
    }
    public void setTotalSizeCATV(Integer totalSizeCATV) {
        this.totalSizeCATV = totalSizeCATV;
    }
    
    @Schema(description="The total size of Telecommunications.  Only first span is desired.")
    private Integer totalSizeTelCom;
    public Integer getTotalSizeTelCom() {
        return totalSizeTelCom;
    }
    public void setTotalSizeTelCom(Integer totalSizeTelCom) {
        this.totalSizeTelCom = totalSizeTelCom;
    }
    
    public PoleSummary() {}
    
    public PoleSummary(Pole p) {
        populateFrom(p);
    }
}
