package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.PoleAnalysisLoadCase;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.poledata.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.papernapkin.liana.xml.sax.AbstractDocumentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Philip A. Chapman
 */
class PoleForemanDocumentHandler extends AbstractDocumentHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final String ATTR_FPL_ID = "GIS_ID";
    private static final String TAG_ANALYSIS = "Analysis_Pole";
    private static final String TAG_ANALYSIS_H_LOADING_PER = "Horizontal_Loading_Percent";
    private static final String TAG_ANALYSIS_PASS = "Pass_or_Fail";
    private static final String TAG_ANALYSIS_POLE_CLASS = "Pole_Class";
    private static final String TAG_ANALYSIS_POLE_DESC = "Pole_Description";
    private static final String TAG_ANALYSIS_POLE_LENGTH = "Pole_Length";
    private static final String TAG_ROOT = "PoleForeman";
    private static final String TAG_ANALYSIS_V_LOADING_PER = "Vertical_Loading_Percent";
    private static final String TAG_ANCHOR = "Anchor";
    private static final String TAG_ANCHOR_BEARING = "Bearing";
    private static final String TAG_ANCHOR_GUY = "GuyAssc";
    private static final String TAG_ANCHOR_LEAD_LEN = "LeadLength";
    private static final String TAG_ANCHOR_STRAND_DIA = "StrandDiameter";
    private static final String TAG_ANCHORS = "Anchors"; // Contains a list of anchors and is a result tag in "Components"
    private static final String TAG_CATV = "CATV";
    private static final String TAG_CIRCUIT = "Circuit";
    private static final String TAG_CIRCUIT_CONDUCTOR = "Conductor";
    private static final String TAG_CIRCUIT_FRAMING = "Framing";
    private static final String TAG_CIRCUIT_MULTIPLEX = "Multiplex";
    private static final String TAG_CIRCUIT_NEUTRAL = "Neutral";
    private static final String TAG_CIRCUIT_PHASES = "Phases";
    private static final String TAG_CIRCUIT_PRIMARY = "Primary";
    private static final String TAG_CIRCUIT_SECONDARY = "Secondary";
    private static final String TAG_CIRCUIT_WIRE_COUNT = "WireCount";
    private static final String TAG_COMMUNICATION = "Communication";
    private static final String TAG_COMPONENTS = "Components";
    private static final String TAG_DESCRIPTION = "Description";
    private static final String TAG_DIAMETER = "Diameter";
    private static final String TAG_EQUIPMENT = "Equipment";
    private static final String TAG_EQUIPMENTS = "Equipments";
    private static final String TAG_HEIGHT = "Height";
    private static final String TAG_LIGHT = "Light"; // Can be for equipment or light
    private static final String TAG_LIGHTS = "Lights";
    private static final String TAG_LOAD_CASE = "LoadCase";
    private static final String TAG_LOAD_CASE_ICE = "Ice";
    private static final String TAG_LOAD_CASE_RULE = "NESC_Rule";
    private static final String TAG_LOAD_CASE_TEMP = "Temp";
    private static final String TAG_LOAD_CASE_WIND = "Wind";
    private static final String TAG_POWER = "Power";
    private static final String TAG_RESULTS_DOWN_GUYS = "DownGuys";
    private static final String TAG_RESULTS_BRACKETS = "Brackets";
    private static final String TAG_RESULTS_INSULATORS = "Insulators";
    private static final String TAG_RISER = "Riser";
    private static final String TAG_RISERS = "Risers";
    private static final String TAG_SPAN = "Span";
    private static final String TAG_SPAN_BEARING = "SpanBearing";
    private static final String TAG_SPAN_LENGTH = "SpanLength";
    private static final String TAG_SPANS = "Spans";
    private static final String TAG_TELCO = "TELCO";
    private static final String TAG_TIMESTAMP = "Timestamp";
    private static final String TAG_TYPE = "Type"; // Can be for equipment or light
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    private final PoleInspection inspection;
    private final ProcessListener listener;
    private final Pole pole;

    private PoleAnchor anchor;
    private int anchorIndex = -1;
    private CommunicationsCable commCable;
    private int commIndex = -1;
    private PoleEquipment equip;
    private int equipIndex = -1;
    private boolean inComponents = false;
    private PoleLight light;
    private int lightIndex = -1;
    private PoleAnalysisLoadCase loadCase;
    private boolean poleForemanXML;
    private PowerCircuit power;
    private NeutralCable neutralPowerCable;
    private PrimaryCable primaryPowerCable;
    private int riserIndex = -1;
    private SecondaryCable secondaryPowerCable;
    private PoleSpan span;
    private int spanIndex = -1;

    public PoleForemanDocumentHandler(ProcessListener listener, Pole pole, PoleInspection inspection) {
        this.inspection = inspection;
        this.listener = listener;
        this.pole = pole;
    }

    @Override
    protected void _endElement(String uri, String localName, String qName) throws SAXException {
        switch (localName) {
            case TAG_ANALYSIS_H_LOADING_PER:
                inspection.setHorizontalLoadingPercent(intFromBuffer());
                break;
            case TAG_ANALYSIS_PASS:
                inspection.setPassedAnalysis(passOrFail());
                break;
            case TAG_ANALYSIS_POLE_CLASS:
                pole.setPoleClass(stringFromBuffer());
                break;
            case TAG_ANALYSIS_POLE_DESC:
                pole.setDescription(stringFromBuffer());
                break;
            case TAG_ANALYSIS_POLE_LENGTH:
                pole.setLength(intFromBuffer());
                break;
            case TAG_ANALYSIS_V_LOADING_PER:
                inspection.setVerticalLoadingPercent(intFromBuffer());
                break;
            case TAG_ANCHOR:
                checkNotNullAtEndTag(localName, anchor);
                anchor = null;
                break;
            case TAG_ANCHORS:
                if (inComponents) {
                    // This is a tag which holds a alanysis result value
                    inspection.setAnchorsPass(passOrFail());
                } else {
                    // This is a tag which encompasses anchor info.
                    checkIndexAtEndTag(localName, anchorIndex);
                    anchorIndex = -2; // We should only see this collection once.  -2 signifies having completed this collection already.
                }
                break;
            case TAG_ANCHOR_BEARING:
                checkNotNullAtEndTag(localName, anchor);
                anchor.setBearing(stringFromBuffer());
                break;
            case TAG_ANCHOR_GUY:
                checkNotNullAtEndTag(localName, anchor);
                anchor.setGuyAssc(PoleAnchor.GuyAssc.fromXMLString(stringFromBuffer()));
                break;
            case TAG_ANCHOR_LEAD_LEN:
                checkNotNullAtEndTag(localName, anchor);
                anchor.setLeadLength(stringFromBuffer());
                break;
            case TAG_ANCHOR_STRAND_DIA:
                checkNotNullAtEndTag(localName, anchor);
                anchor.setStrandDiameter(stringFromBuffer());
                break;
            case TAG_CATV:
            case TAG_TELCO:
                checkNotNullAtEndTag(localName, commCable);
                commCable = null;
                break;
            case TAG_CIRCUIT:
                checkNotNullAtEndTag(localName, power);
                power = null;
                break;
            case TAG_CIRCUIT_CONDUCTOR:
                if (neutralPowerCable != null) {
                    neutralPowerCable.setConductor(stringFromBuffer());
                } else if (primaryPowerCable != null) {
                    primaryPowerCable.setConductor(stringFromBuffer());
                } else if (secondaryPowerCable != null) {
                    secondaryPowerCable.setConductor(stringFromBuffer());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_CIRCUIT_FRAMING:
                checkNotNullAtEndTag(localName, primaryPowerCable);
                primaryPowerCable.setFraming(stringFromBuffer());
                break;
            case TAG_CIRCUIT_MULTIPLEX:
                checkNotNullAtEndTag(localName, secondaryPowerCable);
                secondaryPowerCable.setMultiplex(boolFromBuffer());
                break;
            case TAG_CIRCUIT_NEUTRAL:
                checkNotNullAtEndTag(localName, neutralPowerCable);
                neutralPowerCable = null;
                break;
            case TAG_CIRCUIT_PHASES:
                checkNotNullAtEndTag(localName, primaryPowerCable);
                primaryPowerCable.setPhases(intFromBuffer());
                break;
            case TAG_CIRCUIT_PRIMARY:
                checkNotNullAtEndTag(localName, primaryPowerCable);
                primaryPowerCable = null;
                break;
            case TAG_CIRCUIT_SECONDARY:
                checkNotNullAtEndTag(localName, secondaryPowerCable);
                secondaryPowerCable = null;
                break;
            case TAG_CIRCUIT_WIRE_COUNT:
                checkNotNullAtEndTag(localName, secondaryPowerCable);
                secondaryPowerCable.setWireCount(intFromBuffer());
                break;
            case TAG_COMMUNICATION:
                checkIndexAtEndTag(localName, commIndex);
                commIndex = -1;
                break;
            case TAG_COMPONENTS:
                if (inComponents) {
                    inComponents = false;
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_DESCRIPTION:
                if (equip != null) {
                    equip.setDescription(stringFromBuffer());
                } else if (light != null) {
                    light.setDescription(stringFromBuffer());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_DIAMETER:
                checkNotNullAtEndTag(localName, commCable);
                commCable.setDiameter(floatFromBuffer());
                break;
            case TAG_EQUIPMENT:
                checkNotNullAtEndTag(localName, equip);
                equip = null;
                break;
            case TAG_EQUIPMENTS:
                checkNotNullAtEndTag(localName, equipIndex);
                equipIndex = -2; // We should only see this collection once.  -2 signifies having completed this collection already.
                break;
            case TAG_HEIGHT:
                checkNotNullAtEndTag(localName, commCable);
                commCable.setHeight(floatFromBuffer());
                break;
            case TAG_LIGHT:
                checkNotNullAtEndTag(localName, light);
                light = null;
                break;
            case TAG_LIGHTS:
                checkIndexAtEndTag(localName, lightIndex);
                lightIndex = -2; // We should only see this collection once.  -2 signifies having completed this collection already.
                break;
            case TAG_LOAD_CASE:
                checkNotNullAtEndTag(localName, loadCase);
                loadCase = null;
                break;
            case TAG_LOAD_CASE_ICE:
                checkNotNullAtEndTag(localName, loadCase);
                loadCase.setIce(stringFromBuffer());
                break;
            case TAG_LOAD_CASE_RULE:
                checkNotNullAtEndTag(localName, loadCase);
                loadCase.setNescRule(stringFromBuffer());
                break;
            case TAG_LOAD_CASE_TEMP:
                checkNotNullAtEndTag(localName, loadCase);
                loadCase.setTemperature(stringFromBuffer());
                break;
            case TAG_LOAD_CASE_WIND:
                checkNotNullAtEndTag(localName, loadCase);
                loadCase.setWind(stringFromBuffer());
                break;
            case TAG_RESULTS_BRACKETS:
                if (inComponents) {
                    inspection.setBracketsPass(passOrFail());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_RESULTS_DOWN_GUYS:
                if (inComponents) {
                    inspection.setDownGuysPass(passOrFail());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_RESULTS_INSULATORS:
                if (inComponents) {
                    inspection.setInsulatorsPass(passOrFail());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            case TAG_RISERS:
                checkIndexAtEndTag(localName, riserIndex);
                riserIndex = -2; // We should only see this collection once.  -2 signifies having completed this collection already.
                break;
            case TAG_SPAN:
                checkNotNullAtEndTag(localName, span);
                span = null;
                break;
            case TAG_SPANS:
                checkIndexAtEndTag(localName, spanIndex);
                spanIndex = -2; // We should only see this collection once.  -2 signifies having completed this collection already.
                break;
            case TAG_SPAN_BEARING:
                checkNotNullAtEndTag(localName, span);
                span.setBearing(stringFromBuffer());
                break;
            case TAG_SPAN_LENGTH:
                checkNotNullAtEndTag(localName, span);
                span.setLength(stringFromBuffer());
                break;
            case TAG_TIMESTAMP:
                String s = stringFromBuffer();
                if (s == null || s.isEmpty()) {
                    inspection.setDateOfAnalysis(null);
                } else {
                    inspection.setDateOfAnalysis(LocalDate.parse(s, TIMESTAMP_FORMATTER));
                }
                break;
            case TAG_TYPE:
                if (equip != null) {
                    equip.setType(stringFromBuffer());
                } else if (light != null) {
                    light.setType(stringFromBuffer());
                } else if (riserIndex > -2) {
                    setRiserType(riserIndex + 1, stringFromBuffer());
                } else {
                    throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
                }
                break;
            default:
                LOGGER.info("Skipping unhandled end tag {}", localName);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (TAG_ROOT.equals(localName)) {
            poleForemanXML = true;
        } else if (poleForemanXML) {
            switch (localName) {
                case TAG_ANALYSIS:
                    String fplid = attributes.getValue(ATTR_FPL_ID);
                    if (fplid != null) {
                        fplid = fplid.trim();
                    }
                    if (!Objects.equals(pole.getFPLId(), fplid)) {
                        throw new SAXException(String.format("The pole's FPL ID \"%s\" does not match the GIS_ID attribute \"%s\"", pole.getFPLId(), fplid));
                    }
                    break;
                case TAG_ANCHOR:
                    checkNullAtStartTag(localName, anchor);
                    ensureAnchor(anchorIndex + 1);
                    break;
                case TAG_ANCHORS:
                    if (!inComponents) {
                        checkIndexAtStartOfCollection(localName, anchorIndex);
                    }
                    break;
                case TAG_CATV:
                case TAG_TELCO:
                    checkNotNullAtStartTag(localName, span);
                    checkNullAtStartTag(localName, commCable);
                    checkIndexInsideOfCollection(localName, commIndex);
                    ensureCommunicationsCable(
                            TAG_CATV.equals(localName) ? CommunicationsCable.Type.CaTV : CommunicationsCable.Type.Telco,
                            commIndex + 1
                    );
                    break;
                case TAG_CIRCUIT:
                    checkNotNullAtStartTag(localName, span);
                    checkNullAtStartTag(localName, power);
                    ensurePowerCircuit();
                    break;
                case TAG_CIRCUIT_NEUTRAL:
                    checkNotNullAtStartTag(localName, span);
                    checkNotNullAtStartTag(localName, power);
                    checkNullAtStartTag(localName, neutralPowerCable);
                    checkNullAtStartTag(localName, primaryPowerCable);
                    checkNullAtStartTag(localName, secondaryPowerCable);
                    ensureNeutralCircuitCable();
                    break;
                case TAG_CIRCUIT_PRIMARY:
                    checkNotNullAtStartTag(localName, span);
                    checkNotNullAtStartTag(localName, power);
                    checkNullAtStartTag(localName, neutralPowerCable);
                    checkNullAtStartTag(localName, primaryPowerCable);
                    checkNullAtStartTag(localName, secondaryPowerCable);
                    ensurePrimaryCircuitCable();
                    break;
                case TAG_CIRCUIT_SECONDARY:
                    checkNotNullAtStartTag(localName, span);
                    checkNotNullAtStartTag(localName, power);
                    checkNullAtStartTag(localName, neutralPowerCable);
                    checkNullAtStartTag(localName, primaryPowerCable);
                    checkNullAtStartTag(localName, secondaryPowerCable);
                    ensureSecondaryCircuitCable();
                    break;
                case TAG_COMMUNICATION:
                    checkNotNullAtStartTag(localName, span);
                    checkNullAtStartTag(localName, commCable);
                    checkIndexAtStartOfCollection(localName, commIndex);
                    break;
                case TAG_COMPONENTS:
                    if (inComponents) {
                        throw new SAXException(String.format("Start tag \"%s\" unexpected.", localName));
                    } else {
                        inComponents = true;
                    }
                    break;
                case TAG_EQUIPMENT:
                    checkNullAtStartTag(localName, equip);
                    checkIndexInsideOfCollection(localName, equipIndex);
                    ensureEquipment(equipIndex + 1);
                    break;
                case TAG_EQUIPMENTS:
                    checkIndexAtStartOfCollection(localName, equipIndex);
                    break;
                case TAG_LIGHT:
                    checkIndexInsideOfCollection(localName, lightIndex);
                    ensureLight(lightIndex + 1);
                    break;
                case TAG_LIGHTS:
                    checkIndexAtStartOfCollection(localName, lightIndex);
                    break;
                case TAG_LOAD_CASE:
                    checkNullAtStartTag(localName, loadCase);
                    ensureLoadCase();
                    break;
                case TAG_RISERS:
                    checkIndexAtStartOfCollection(localName, riserIndex);
                    break;
                case TAG_SPAN:
                    checkNullAtStartTag(localName, span);
                    checkIndexInsideOfCollection(localName, spanIndex);
                    ensureSpan(spanIndex + 1);
                    break;
                case TAG_SPANS:
                    // We should only see this once
                    checkIndexAtStartOfCollection(localName, spanIndex);
                    break;
                default:
                    LOGGER.info("Skipping unhandled start tag {}", localName);
            }
        } else {
            throw new SAXException("This is not a PoleForeman XML file.");
        }
    }

    private void checkIndexAtEndTag(String localName, int index) throws SAXException {
        if (index < -1) {
            throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
        }
    }

    private void checkIndexAtStartOfCollection(String localName, int index) throws SAXException {
        if (index != -1) {
            throw new SAXException(String.format("Start tag \"%s\" unexpected.", localName));
        }
    }

    private void checkIndexInsideOfCollection(String localName, int index) throws SAXException {
        if (index < -1) {
            throw new SAXException(String.format("Start tag \"%s\" unexpected.", localName));
        }
    }

    private void checkNotNullAtStartTag(String localName, Object obj) throws SAXException {
        if (obj == null) {
            throw new SAXException(String.format("Start tag \"%s\" found out of order.", localName));
        }
    }

    private void checkNotNullAtEndTag(String localName, Object obj) throws SAXException {
        if (obj == null) {
            throw new SAXException(String.format("End tag \"%s\" unexpected.", localName));
        }
    }

    private void checkNullAtStartTag(String localName, Object obj) throws SAXException {
        if (obj != null) {
            throw new SAXException(String.format("Start tag \"%s\" found out of order.", localName));
        }
    }

    private void ensureAnchor(int index) {
        if (pole.getAnchors() == null) {
            pole.setAnchors(new ArrayList<>(index + 1));
        }
        padList(pole.getAnchors(), index, null);
        PoleAnchor anchor = pole.getAnchors().get(index);
        if (anchor == null) {
            anchor = new PoleAnchor();;
            pole.getAnchors().set(index, anchor);
        }
        this.anchorIndex = index;
        this.anchor = anchor;
    }

    private void setRiserType(int index, String riserType) {
        if (pole.getRisers() == null) {
            pole.setRisers(new ArrayList<>(index + 1));
        }
        padList(pole.getRisers(), index, null);
        pole.getRisers().set(index, riserType);
        this.riserIndex = index;
    }

    private void ensureCommunicationsCable(CommunicationsCable.Type type, int index) {
        if (span.getCommunications() == null) {
            span.setCommunications(new ArrayList<>(index + 1));
        }
        padList(span.getCommunications(), index, null);
        CommunicationsCable cable = span.getCommunications().get(index);
        if (cable == null) {
            cable = new CommunicationsCable();
            span.getCommunications().set(index, cable);
        }
        cable.setType(type);
        this.commIndex = index;
        this.commCable = cable;
    }

    private void ensureEquipment(int index) {
        if (pole.getEquipment() == null) {
            pole.setEquipment(new ArrayList<>(index + 1));
        }
        padList(pole.getEquipment(), index, null);
        PoleEquipment equip = pole.getEquipment().get(index);
        if (equip == null) {
            equip = new PoleEquipment();
            pole.getEquipment().set(index, equip);
        }
        this.equipIndex = index;
        this.equip = equip;
    }

    private void ensureLight(int index) {
        if (pole.getLights() == null) {
            pole.setLights(new ArrayList<>(index + 1));
        }
        padList(pole.getLights(), index, null);
        PoleLight light = pole.getLights().get(index);
        if (light == null) {
            light = new PoleLight();;
            pole.getLights().set(index, light);
        }
        this.lightIndex = index;
        this.light = light;
    }

    private void ensureLoadCase() {
        if (inspection.getLoadCase() == null) {
            inspection.setLoadCase(new PoleAnalysisLoadCase());
        }
        loadCase = inspection.getLoadCase();
    }

    private void ensureNeutralCircuitCable() {
        if (power.getNeutral() == null) {
            power.setNeutral(new NeutralCable());
        }
        this.neutralPowerCable = power.getNeutral();
    }

    private void ensurePowerCircuit() {
        if (span.getPowerCircuit() == null) {
            span.setPowerCircuit(new PowerCircuit());
        }
        this.power = span.getPowerCircuit();
    }

    private void ensureSecondaryCircuitCable() {
        if (power.getSecondary() == null) {
            power.setSecondary(new SecondaryCable());
        }
        this.secondaryPowerCable = power.getSecondary();
    }

    private void ensurePrimaryCircuitCable() {
        if (power.getPrimary() == null) {
            power.setPrimary(new PrimaryCable());
        }
        this.primaryPowerCable = power.getPrimary();
    }

    private void ensureSpan(int index) {
        if (pole.getSpans() == null) {
            pole.setSpans(new ArrayList<>(index + 1));
        }
        while (pole.getSpans().size() <= index) {
            pole.getSpans().add(null);
        }
        PoleSpan span = pole.getSpans().get(index);
        if (span == null) {
            span = new PoleSpan();
            pole.getSpans().set(index, span);
        }
        this.spanIndex = index;
        this.span = span;
    }

    private Boolean passOrFail() throws SAXException {
        String s = stringFromBuffer();
        if (s == null || s.isEmpty()) {
            return null;
        } else {
            if ("Pass".equalsIgnoreCase(s)) {
                return true;
            } else if ("Fail".equalsIgnoreCase(s)) {
                return false;
            } else {
                throw new SAXException(String.format("Unknown pass or fail value \"%s\"", s));
            }
        }
    }
    
    //FIXME: Move to papernapkin utilities
    private <T> void padList(List<T> list, int size, T item) {
        for (int i = list.size(); i <= size; i++) {
            list.add(item);
        }
    }

    //FIXME: Move to parent
    private Boolean boolFromBuffer() throws SAXException {
        String s = stringFromBuffer();
        if (s == null || s.isEmpty()) {
            return null;
        } else {
            return Boolean.valueOf(s);
        }
    }

    //FIXME: Move to parent
    private Float floatFromBuffer() throws SAXException {
        String s = stringFromBuffer();
        if (s == null || s.isEmpty()) {
            return null;
        } else {
            try {
                return Float.valueOf(s);
            } catch (NumberFormatException ex) {
                throw new SAXException(String.format("Unable to parse the value \"%s\" as a float.", s));
            }
        }
    }

    //FIXME: Move to parent
    private Integer intFromBuffer() throws SAXException {
        String s = stringFromBuffer();
        if (s == null || s.isEmpty()) {
            return null;
        } else {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                throw new SAXException(String.format("Unable to parse the value \"%s\" as an integer.", s));
            }
        }
    }

    //FIXME: Move to parent
    private String stringFromBuffer() {
        return textbuffer == null ? null : textbuffer.toString().trim();
    }
}
